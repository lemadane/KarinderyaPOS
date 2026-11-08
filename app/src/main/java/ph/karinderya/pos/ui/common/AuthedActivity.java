package ph.karinderya.pos.ui.common;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import ph.karinderya.pos.KarinderyaApp;
import ph.karinderya.pos.ui.auth.LoginActivity;
import ph.karinderya.pos.ui.trial.TrialExpiredActivity;

/**
 * Base for screens that need a logged-in crew member. After process death Android may recreate
 * any activity directly, so the session is restored (or the user is sent to login) here.
 */
public abstract class AuthedActivity extends AppCompatActivity {

    protected KarinderyaApp app;

    /** Subclasses must return immediately from onCreate when this is false. */
    protected boolean sessionValid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Ui.enableEdgeToEdge(this, true);
        super.onCreate(savedInstanceState);
        app = KarinderyaApp.get(this);
        if (app.trial().check().isBlocked()) {
            sessionValid = false;
            openTrialExpired();
            return;
        }
        sessionValid = app.session().isLoggedIn() || app.session().restore().isPresent();
        if (!sessionValid) {
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
    }

    /** Re-checked on resume so an app left open past the deadline still locks. */
    @Override
    protected void onResume() {
        super.onResume();
        if (sessionValid && app.trial().check().isBlocked()) {
            sessionValid = false;
            openTrialExpired();
        }
    }

    void openTrialExpired() {
        startActivity(new Intent(this, TrialExpiredActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    protected void setupToolbar(MaterialToolbar toolbar, CharSequence title, boolean showUp) {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(showUp);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
