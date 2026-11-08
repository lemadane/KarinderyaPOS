package ph.karinderya.pos.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import ph.karinderya.pos.KarinderyaApp;
import ph.karinderya.pos.R;
import ph.karinderya.pos.data.model.OrderFilter;
import ph.karinderya.pos.databinding.FragmentOrdersBinding;
import ph.karinderya.pos.ui.MainActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Async;
import ph.karinderya.pos.util.SimpleTextWatcher;

public final class OrdersFragment extends Fragment {

     FragmentOrdersBinding binding;
     OrderAdapter adapter;
     String query = "";
     int loadGeneration;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new OrderAdapter(order -> startActivity(new Intent(requireContext(), OrderDetailActivity.class)
                .putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id().toString())));
        binding.orderList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.orderList.setAdapter(adapter);
        binding.filterChips.setOnCheckedStateChangeListener((group, ids) -> load());
        binding.searchInput.addTextChangedListener(new SimpleTextWatcher(text -> {
            query = text;
            load();
        }));
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

     OrderFilter filter() {
        int id = binding.filterChips.getCheckedChipId();
        if (id == R.id.chipToday) {
            return OrderFilter.TODAY;
        }
        return id == R.id.chipAll ? OrderFilter.ALL : OrderFilter.ACTIVE;
    }

     void load() {
        if (binding == null) {
            return;
        }
        KarinderyaApp app = KarinderyaApp.get(requireContext());
        OrderFilter filter = filter();
        String q = query;
        int generation = ++loadGeneration;
        Async.run(() -> app.orders().list(filter, q), orders -> {
            if (binding == null || generation != loadGeneration) {
                return;
            }
            adapter.submitList(orders);
            binding.emptyView.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
            if (getActivity() instanceof MainActivity main) {
                main.refreshChrome();
            }
        }, e -> Ui.showError(requireContext(), e));
    }
}
