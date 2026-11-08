package ph.karinderya.pos.trial;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import ph.karinderya.pos.BuildConfig;

/**
 * Offline time-limited trial counted from installation. The start is the earlier of Android's
 * firstInstallTime (survives "Clear data") and the stored start (survives app updates); the
 * latest time seen is stored to detect the clock being set backwards. Without a server this is
 * a deterrent, not tamper-proof: uninstalling and reinstalling starts a new trial.
 */
public record TrialGuard(
    SharedPreferences prefs, Instant installedAt, Duration length, boolean exempt, Clock clock) {

  static final String PREFS = "trial";
  static final String KEY_STARTED_AT = "started_at";
  static final String KEY_LAST_SEEN_AT = "last_seen_at";
  static final String KEY_EXPIRED = "expired";

  public TrialGuard(Context context) {
    this(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        firstInstallTime(context),
        Duration.ofDays(BuildConfig.TRIAL_DAYS),
        BuildConfig.TRIAL_EXEMPT,
        Clock.systemUTC());
  }

  /** Cheap (SharedPreferences only); safe to call on the main thread. */
  public synchronized TrialStatus check() {
    if (exempt) {
      return TrialStatus.exempt();
    }
    Instant now = clock.instant();
    Instant startedAt = earliest(installedAt, readInstant(KEY_STARTED_AT));
    if (startedAt == null) {
      startedAt = now;
    }
    Instant lastSeenAt = readInstant(KEY_LAST_SEEN_AT);
    boolean expiredBefore = prefs.getBoolean(KEY_EXPIRED, false);

    TrialStatus status = TrialStatus.evaluate(startedAt, lastSeenAt, now, length, expiredBefore);

    SharedPreferences.Editor editor =
        prefs.edit().putLong(KEY_STARTED_AT, startedAt.toEpochMilli());
    if (status.state() == TrialState.EXPIRED) {
      // Latched so winding the clock back afterwards cannot reopen the app.
      editor.putBoolean(KEY_EXPIRED, true);
    }
    if (status.state() != TrialState.CLOCK_ROLLED_BACK
        && (lastSeenAt == null || now.isAfter(lastSeenAt))) {
      editor.putLong(KEY_LAST_SEEN_AT, now.toEpochMilli());
    }
    editor.apply();
    return status;
  }

  Instant readInstant(String key) {
    long millis = prefs.getLong(key, -1);
    return millis < 0 ? null : Instant.ofEpochMilli(millis);
  }

  static Instant earliest(Instant a, Instant b) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return a.isBefore(b) ? a : b;
  }

  static Instant firstInstallTime(Context context) {
    try {
      long millis =
          context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime;
      return millis > 0 ? Instant.ofEpochMilli(millis) : null;
    } catch (PackageManager.NameNotFoundException e) {
      return null;
    }
  }
}
