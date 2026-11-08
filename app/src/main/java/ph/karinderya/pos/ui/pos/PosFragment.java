package ph.karinderya.pos.ui.pos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import ph.karinderya.pos.KarinderyaApp;
import ph.karinderya.pos.R;
import ph.karinderya.pos.data.CartManager;
import ph.karinderya.pos.data.model.MenuItem;
import ph.karinderya.pos.data.repo.MenuRepository;
import ph.karinderya.pos.databinding.FragmentPosBinding;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Async;
import ph.karinderya.pos.util.Money;
import ph.karinderya.pos.util.SimpleTextWatcher;

/** Crew-facing order taking: tap dishes to add, long-press to remove one. */
public final class PosFragment extends Fragment {

     FragmentPosBinding binding;
     CartManager cart;
     PosMenuAdapter adapter;
     List<MenuItem> allItems = List.of();
     String selectedCategory;
     String query = "";
     final Runnable cartListener = this::onCartChanged;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        cart = KarinderyaApp.get(requireContext()).cart();
        adapter = new PosMenuAdapter(cart::quantityOf, cart::add, item -> cart.decrement(item.id()));
        int widthDp = getResources().getConfiguration().screenWidthDp;
        binding.menuList.setLayoutManager(new GridLayoutManager(requireContext(), Math.max(2, widthDp / 170)));
        binding.menuList.setAdapter(adapter);
        binding.searchInput.addTextChangedListener(new SimpleTextWatcher(text -> {
            query = text.trim().toLowerCase(Locale.ROOT);
            applyFilter();
        }));
        binding.categoryChips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) {
                return;
            }
            Chip chip = group.findViewById(ids.get(0));
            selectedCategory = chip.getTag() == null ? null : chip.getTag().toString();
            applyFilter();
        });
        binding.checkoutButton.setOnClickListener(v -> startActivity(new Intent(requireContext(),
                CheckoutActivity.class)));
        cart.addListener(cartListener);
        onCartChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            load();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            load();
        }
    }

    @Override
    public void onDestroyView() {
        cart.removeListener(cartListener);
        binding = null;
        super.onDestroyView();
    }

     void load() {
        KarinderyaApp app = KarinderyaApp.get(requireContext());
        Async.run(() -> app.menu().list(null, null, false), items -> {
            if (binding == null) {
                return;
            }
            allItems = items.stream()
                    .filter(MenuItem::mealOfDay)
                    .collect(Collectors.toList());
            buildCategoryChips();
            applyFilter();
        }, e -> Ui.showError(requireContext(), e));
    }

     void buildCategoryChips() {
        // Fixed tabs first; custom categories follow in the order the sorted items bring them.
        Set<String> categories = new LinkedHashSet<>(MenuRepository.DEFAULT_CATEGORIES);
        allItems.stream().map(MenuItem::category).forEach(categories::add);
        if (selectedCategory != null && !categories.contains(selectedCategory)) {
            selectedCategory = null;
        }
        binding.categoryChips.removeAllViews();
        addChip(getString(R.string.all), null);
        for (String category : categories) {
            addChip(category, category);
        }
    }

     void addChip(String label, String category) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filter, binding.categoryChips, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(category);
        binding.categoryChips.addView(chip);
        if ((category == null && selectedCategory == null) || (category != null && category.equals(selectedCategory))) {
            chip.setChecked(true);
        }
    }

     void applyFilter() {
        List<MenuItem> filtered = allItems.stream()
                .filter(i -> selectedCategory == null || selectedCategory.equals(i.category()))
                .filter(i -> query.isEmpty() || i.name().toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
        adapter.submitList(filtered);
        binding.emptyView.setText(allItems.isEmpty() ? R.string.empty_menu_pos : R.string.no_matching_meals);
        binding.emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

     void onCartChanged() {
        if (binding == null) {
            return;
        }
        adapter.notifyItemRangeChanged(0, adapter.getItemCount());
        boolean empty = cart.isEmpty();
        binding.cartBar.setVisibility(empty ? View.GONE : View.VISIBLE);
        int qty = cart.totalQuantity();
        binding.cartSummary.setText(getResources().getQuantityString(R.plurals.cart_summary, qty, qty,
                Money.format(cart.subtotalCents())));
    }
}
