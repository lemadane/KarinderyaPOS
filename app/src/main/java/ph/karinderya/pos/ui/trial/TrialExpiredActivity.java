package ph.karinderya.pos.ui.trial;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import ph.karinderya.pos.BuildConfig;
import ph.karinderya.pos.KarinderyaApp;
import ph.karinderya.pos.R;
import ph.karinderya.pos.databinding.ActivityTrialExpiredBinding;
import ph.karinderya.pos.trial.TrialState;
import ph.karinderya.pos.trial.TrialStatus;
import ph.karinderya.pos.ui.auth.LoginActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Async;

/** Lock screen shown when the trial has ended; only exporting a backup is allowed. */
public final class TrialExpiredActivity extends AppCompatActivity {

  static final DateTimeFormatter DATE = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

  ActivityTrialExpiredBinding binding;
  KarinderyaApp app;

  final ActivityResultLauncher<String> exportLauncher =
      registerForActivityResult(
          new ActivityResultContracts.CreateDocument("application/json"),
          uri -> {
            if (uri != null) {
              exportTo(uri);
            }
          });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Ui.enableEdgeToEdge(this, false);
    super.onCreate(savedInstanceState);
    binding = ActivityTrialExpiredBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
    Ui.applyInsets(binding.root, null);
    app = KarinderyaApp.get(this);

    binding.exportButton.setOnClickListener(
        v -> exportLauncher.launch("karinderya-backup-" + LocalDate.now() + ".json"));
    binding.retryButton.setOnClickListener(v -> refresh());
    binding.closeButton.setOnClickListener(v -> finishAffinity());
  }

  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }

  void refresh() {
    TrialStatus status = app.trial().check();
    if (!status.isBlocked()) {
      // e.g. the device clock was corrected.
      startActivity(
          new Intent(this, LoginActivity.class)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
      finish();
      return;
    }
    String appName = getString(R.string.app_name);
    boolean clock = status.state() == TrialState.CLOCK_ROLLED_BACK;
    binding.title.setText(clock ? R.string.trial_clock_title : R.string.trial_expired_title);
    binding.message.setText(
        clock
            ? getString(R.string.trial_clock_message, appName)
            : getString(
                R.string.trial_expired_message,
                appName,
                format(status.startedAt()),
                BuildConfig.TRIAL_DAYS,
                format(status.expiresAt())));
    binding.retryButton.setVisibility(clock ? View.VISIBLE : View.GONE);
  }

  void exportTo(Uri uri) {
    binding.exportButton.setEnabled(false);
    Async.run(
        () -> {
          try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) {
              throw new IOException("Unable to open file");
            }
            return app.backup().export(out);
          }
        },
        data -> {
          binding.exportButton.setEnabled(true);
          Ui.toast(
              this,
              getString(R.string.export_done, data.menuItems().size(), data.orders().size()));
        },
        e -> {
          binding.exportButton.setEnabled(true);
          Ui.showError(this, e);
        });
  }

  static String format(Instant instant) {
    return DATE.format(instant.atZone(ZoneId.systemDefault()));
  }
}
