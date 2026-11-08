package ph.karinderya.pos.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import ph.karinderya.pos.KarinderyaApp;
import ph.karinderya.pos.R;
import ph.karinderya.pos.databinding.ActivityRegisterBinding;
import ph.karinderya.pos.ui.MainActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Async;

public final class RegisterActivity extends AppCompatActivity {

     ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Ui.enableEdgeToEdge(this, true);
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.applyInsets(binding.root, binding.toolbar);
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.register_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.registerButton.setOnClickListener(v -> register());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

     void register() {
        String fullName = text(binding.fullNameInput.getText());
        String username = text(binding.usernameInput.getText());
        String password = text(binding.passwordInput.getText());
        String confirm = text(binding.confirmInput.getText());
        String required = getString(R.string.error_required);
        binding.fullNameLayout.setError(fullName.isBlank() ? required : null);
        binding.usernameLayout.setError(username.isBlank() ? required : null);
        binding.passwordLayout.setError(password.isEmpty() ? required : null);
        binding.confirmLayout.setError(!password.equals(confirm) ? getString(R.string.error_password_mismatch) : null);
        if (fullName.isBlank() || username.isBlank() || password.isEmpty() || !password.equals(confirm)) {
            return;
        }
        KarinderyaApp app = KarinderyaApp.get(this);
        binding.registerButton.setEnabled(false);
        Async.run(() -> app.users().register(username, fullName, password), user -> {
            app.session().login(user);
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }, e -> {
            binding.registerButton.setEnabled(true);
            Ui.showError(this, e);
        });
    }

     static String text(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
