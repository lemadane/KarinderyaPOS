package ph.karinderya.pos.ui.orders;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;

import java.util.function.Consumer;
import java.util.stream.Collectors;

import ph.karinderya.pos.R;
import ph.karinderya.pos.data.model.Order;
import ph.karinderya.pos.databinding.ItemOrderBinding;
import ph.karinderya.pos.ui.common.BindingViewHolder;
import ph.karinderya.pos.ui.common.KeyDiffCallback;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Money;
import ph.karinderya.pos.util.ReceiptFormatter;

public final class OrderAdapter extends ListAdapter<Order, BindingViewHolder<ItemOrderBinding>> {

     final Consumer<Order> onClick;

    public OrderAdapter(Consumer<Order> onClick) {
        super(new KeyDiffCallback<>(Order::id));
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public BindingViewHolder<ItemOrderBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BindingViewHolder<>(ItemOrderBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BindingViewHolder<ItemOrderBinding> holder, int position) {
        Order order = getItem(position);
        ItemOrderBinding b = holder.binding;
        Context context = b.getRoot().getContext();
        b.orderNo.setText(context.getString(R.string.order_no, order.orderNo()));
        b.status.setText(order.status().label());
        b.status.setBackgroundTintList(null);
        b.status.setBackgroundColor(Ui.statusColor(context, order.status()));
        StringBuilder meta = new StringBuilder(order.orderType().label());
        if (order.customerLabel() != null) {
            meta.append(" · ").append(order.customerLabel());
        }
        meta.append(" · ").append(ReceiptFormatter.formatDateTime(order.createdAt()))
                .append(" · ").append(order.paymentMethod().label());
        b.meta.setText(meta);
        b.items.setText(order.items().stream()
                .map(i -> i.quantity() + "× " + i.name())
                .collect(Collectors.joining(", ")));
        b.total.setText(Money.format(order.totalCents()));
        b.getRoot().setOnClickListener(v -> onClick.accept(order));
    }
}
