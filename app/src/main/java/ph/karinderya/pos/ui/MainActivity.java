package ph.karinderya.pos.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.snackbar.Snackbar;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import ph.karinderya.pos.R;
import ph.karinderya.pos.databinding.ActivityMainBinding;
import ph.karinderya.pos.trial.TrialStatus;
import ph.karinderya.pos.ui.common.AuthedActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.ui.menu.MenuFragment;
import ph.karinderya.pos.ui.more.MoreFragment;
import ph.karinderya.pos.ui.orders.OrdersFragment;
import ph.karinderya.pos.ui.pos.PosFragment;
import ph.karinderya.pos.util.Async;

public final class MainActivity extends AuthedActivity {

     static final String STATE_TAB = "tab";

     final Map<Integer, Supplier<Fragment>> tabs = new LinkedHashMap<>();
     ActivityMainBinding binding;
     int currentTab = R.id.nav_pos;
     final Runnable cartListener = this::invalidateOptionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sessionValid) {
            return;
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.applyInsets(binding.root, binding.toolbar);
        setupToolbar(binding.toolbar, app.settings().storeName(), false);

        tabs.put(R.id.nav_pos, PosFragment::new);
        tabs.put(R.id.nav_orders, OrdersFragment::new);
        tabs.put(R.id.nav_menu, MenuFragment::new);
        tabs.put(R.id.nav_more, MoreFragment::new);

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt(STATE_TAB, R.id.nav_pos);
        }
        showTab(currentTab);
        binding.bottomNav.setSelectedItemId(currentTab);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        app.cart().addListener(cartListener);
        if (savedInstanceState == null) {
            showTrialWarning();
        }
    }

    void showTrialWarning() {
        TrialStatus trial = app.trial().check();
        if (trial.shouldWarn()) {
            int days = (int) trial.daysLeft();
            Snackbar.make(binding.getRoot(),
                            getResources().getQuantityString(R.plurals.trial_days_left, days, days),
                            Snackbar.LENGTH_INDEFINITE)
                    .setAnchorView(binding.bottomNav)
                    .setAction(R.string.ok, v -> { })
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionValid) {
            return;
        }
        updateTitle();
        refreshActiveBadge();
    }

    @Override
    protected void onDestroy() {
        if (sessionValid) {
            app.cart().removeListener(cartListener);
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, currentTab);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pos, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_clear_cart).setVisible(currentTab == R.id.nav_pos && !app.cart().isEmpty());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_cart) {
            Ui.confirm(this, getString(R.string.clear_cart_confirm), () -> app.cart().clear());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Called by child screens when data shown in the toolbar or badges may have changed. */
    public void refreshChrome() {
        updateTitle();
        refreshActiveBadge();
    }

     void showTab(int tabId) {
        currentTab = tabId;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true);
        for (Map.Entry<Integer, Supplier<Fragment>> entry : tabs.entrySet()) {
            String tag = "tab_" + entry.getKey();
            Fragment fragment = fm.findFragmentByTag(tag);
            if (entry.getKey() == tabId) {
                if (fragment == null) {
                    tx.add(R.id.fragmentContainer, entry.getValue().get(), tag);
                } else {
                    tx.show(fragment);
                }
            } else if (fragment != null) {
                tx.hide(fragment);
            }
        }
        tx.commit();
        updateTitle();
        invalidateOptionsMenu();
    }

     void updateTitle() {
        if (getSupportActionBar() == null) {
            return;
        }
        int titleRes = currentTab == R.id.nav_orders ? R.string.nav_orders
                : currentTab == R.id.nav_menu ? R.string.nav_menu
                : currentTab == R.id.nav_more ? R.string.nav_more : 0;
        getSupportActionBar().setTitle(titleRes == 0 ? app.settings().storeName() : getString(titleRes));
        getSupportActionBar().setSubtitle(app.session().requireUser().fullName());
    }

     void refreshActiveBadge() {
        Async.run(() -> app.orders().countActive(), count -> {
            if (count > 0) {
                binding.bottomNav.getOrCreateBadge(R.id.nav_orders).setNumber(count);
            } else {
                binding.bottomNav.removeBadge(R.id.nav_orders);
            }
        }, e -> { });
    }
}
