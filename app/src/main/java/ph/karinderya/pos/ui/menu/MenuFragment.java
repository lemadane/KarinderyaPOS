package ph.karinderya.pos.ui.menu;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import ph.karinderya.pos.KarinderyaApp;
import ph.karinderya.pos.databinding.FragmentMenuBinding;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Async;
import ph.karinderya.pos.util.SimpleTextWatcher;

/** Menu management: add / edit dishes with photo and price, and toggle "available today". */
public final class MenuFragment extends Fragment {

     FragmentMenuBinding binding;
     MenuManageAdapter adapter;
     String query = "";
     int loadGeneration;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        KarinderyaApp app = KarinderyaApp.get(requireContext());
        adapter = new MenuManageAdapter(
                item -> startActivity(new Intent(requireContext(), MenuItemEditActivity.class)
                        .putExtra(MenuItemEditActivity.EXTRA_ITEM_ID, item.id().toString())),
                (item, available) -> Async.run(() -> {
                    app.menu().setAvailable(item.id(), available);
                    return true;
                }, ok -> load(), e -> Ui.showError(requireContext(), e)));
        binding.menuList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.menuList.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        binding.menuList.setAdapter(adapter);
        binding.searchInput.addTextChangedListener(new SimpleTextWatcher(text -> {
            query = text;
            load();
        }));
        binding.addButton.setOnClickListener(v -> startActivity(new Intent(requireContext(),
                MenuItemEditActivity.class)));
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
        binding = null;
        super.onDestroyView();
    }

     void load() {
        if (binding == null) {
            return;
        }
        KarinderyaApp app = KarinderyaApp.get(requireContext());
        String q = query;
        int generation = ++loadGeneration;
        Async.run(() -> app.menu().list(q, null, false), items -> {
            if (binding == null || generation != loadGeneration) {
                return;
            }
            adapter.submitList(items);
            binding.emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        }, e -> Ui.showError(requireContext(), e));
    }
}
