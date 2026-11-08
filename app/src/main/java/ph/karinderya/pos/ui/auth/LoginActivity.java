package ph.karinderya.pos.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;

import ph.karinderya.pos.KarinderyaApp;
import ph.karinderya.pos.R;
import ph.karinderya.pos.databinding.ActivityLoginBinding;
import ph.karinderya.pos.ui.MainActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.ui.trial.TrialExpiredActivity;
import ph.karinderya.pos.util.Async;

public final class LoginActivity extends AppCompatActivity {

     ActivityLoginBinding binding;
     KarinderyaApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.enableEdgeToEdge(this, false);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.applyInsets(binding.root, null);
        app = KarinderyaApp.get(this);
        if (app.trial().check().isBlocked()) {
            startActivity(new Intent(this, TrialExpiredActivity.class));
            finish();
            return;
        }

        binding.loginButton.setOnClickListener(v -> attemptLogin());
        binding.passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
        binding.registerButton.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        setBusy(true);
        Async.run(() -> app.session().restore(), user -> {
            if (user.isPresent()) {
                openMain();
            } else {
                setBusy(false);
            }
        }, e -> setBusy(false));
    }

     void attemptLogin() {
        String username = text(binding.usernameInput.getText());
        String password = text(binding.passwordInput.getText());
        binding.usernameLayout.setError(username.isBlank() ? getString(R.string.error_required) : null);
        binding.passwordLayout.setError(password.isEmpty() ? getString(R.string.error_required) : null);
        if (username.isBlank() || password.isEmpty()) {
            return;
        }
        setBusy(true);
        Async.run(() -> app.users().login(username, password), user -> {
            if (user.isPresent()) {
                app.session().login(user.get());
                openMain();
            } else {
                setBusy(false);
                binding.passwordLayout.setError(getString(R.string.error_invalid_login));
            }
        }, e -> {
            setBusy(false);
            Ui.showError(this, e);
        });
    }

     void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

     void setBusy(boolean busy) {
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!busy);
        binding.registerButton.setEnabled(!busy);
    }

     static String text(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
