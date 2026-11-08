package ph.karinderya.pos.ui.more;

import android.os.Bundle;

import ph.karinderya.pos.R;
import ph.karinderya.pos.databinding.ActivityChangePasswordBinding;
import ph.karinderya.pos.ui.common.AuthedActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Async;

public final class ChangePasswordActivity extends AuthedActivity {

     ActivityChangePasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sessionValid) {
            return;
        }
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.applyInsets(binding.root, binding.toolbar);
        setupToolbar(binding.toolbar, getString(R.string.change_password), true);
        binding.saveButton.setOnClickListener(v -> save());
    }

     void save() {
        String current = String.valueOf(binding.currentInput.getText());
        String next = String.valueOf(binding.newPasswordInput.getText());
        String confirm = String.valueOf(binding.confirmInput.getText());
        binding.currentLayout.setError(current.isEmpty() ? getString(R.string.error_required) : null);
        binding.newPasswordLayout.setError(next.isEmpty() ? getString(R.string.error_required) : null);
        binding.confirmLayout.setError(!next.equals(confirm) ? getString(R.string.error_password_mismatch) : null);
        if (current.isEmpty() || next.isEmpty() || !next.equals(confirm)) {
            return;
        }
        binding.saveButton.setEnabled(false);
        Async.run(() -> app.users().changePassword(app.session().requireUser().id(), current, next), ok -> {
            binding.saveButton.setEnabled(true);
            if (ok) {
                Ui.toast(this, getString(R.string.password_changed));
                finish();
            } else {
                binding.currentLayout.setError(getString(R.string.error_wrong_password));
            }
        }, e -> {
            binding.saveButton.setEnabled(true);
            Ui.showError(this, e);
        });
    }
}
