package ph.karinderya.pos.trial;

import java.time.Duration;
import java.time.Instant;

/** Result of a trial check; {@link #evaluate} is pure so it can be unit tested. */
public record TrialStatus(Instant startedAt, Instant expiresAt, long daysLeft, TrialState state) {

  /** Small clock corrections (NTP sync, manual tweaks) shouldn't lock the crew out. */
  public static final Duration ROLLBACK_TOLERANCE = Duration.ofHours(1);

  public static final int WARNING_DAYS = 7;

  public static TrialStatus exempt() {
    return new TrialStatus(null, null, Long.MAX_VALUE, TrialState.EXEMPT);
  }

  public static TrialStatus evaluate(
      Instant startedAt, Instant lastSeenAt, Instant now, Duration length, boolean expiredBefore) {
    Instant expiresAt = startedAt.plus(length);
    if (expiredBefore || !now.isBefore(expiresAt)) {
      return new TrialStatus(startedAt, expiresAt, 0, TrialState.EXPIRED);
    }
    long daysLeft = ceilDays(Duration.between(now, expiresAt));
    if (lastSeenAt != null && now.isBefore(lastSeenAt.minus(ROLLBACK_TOLERANCE))) {
      return new TrialStatus(startedAt, expiresAt, daysLeft, TrialState.CLOCK_ROLLED_BACK);
    }
    return new TrialStatus(startedAt, expiresAt, daysLeft, TrialState.ACTIVE);
  }

  public boolean isBlocked() {
    return state.isBlocked();
  }

  public boolean shouldWarn() {
    return state == TrialState.ACTIVE && daysLeft <= WARNING_DAYS;
  }

  static long ceilDays(Duration remaining) {
    long days = remaining.toDays();
    return remaining.minusDays(days).isZero() ? days : days + 1;
  }
}
