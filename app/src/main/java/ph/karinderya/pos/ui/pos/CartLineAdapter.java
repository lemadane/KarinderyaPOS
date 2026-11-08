package ph.karinderya.pos.ui.pos;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;

import java.util.UUID;
import java.util.function.Consumer;

import ph.karinderya.pos.data.model.CartLine;
import ph.karinderya.pos.databinding.ItemCartLineBinding;
import ph.karinderya.pos.ui.common.BindingViewHolder;
import ph.karinderya.pos.ui.common.KeyDiffCallback;
import ph.karinderya.pos.util.Money;

public final class CartLineAdapter extends ListAdapter<CartLine, BindingViewHolder<ItemCartLineBinding>> {

     final Consumer<UUID> onIncrement;
     final Consumer<UUID> onDecrement;

    public CartLineAdapter(Consumer<UUID> onIncrement, Consumer<UUID> onDecrement) {
        super(new KeyDiffCallback<>(CartLine::menuItemId));
        this.onIncrement = onIncrement;
        this.onDecrement = onDecrement;
    }

    @NonNull
    @Override
    public BindingViewHolder<ItemCartLineBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BindingViewHolder<>(ItemCartLineBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BindingViewHolder<ItemCartLineBinding> holder, int position) {
        CartLine line = getItem(position);
        ItemCartLineBinding b = holder.binding;
        b.name.setText(line.name());
        b.unitPrice.setText(Money.format(line.unitPriceCents()));
        b.quantity.setText(String.valueOf(line.quantity()));
        b.lineTotal.setText(Money.format(line.lineTotalCents()));
        b.plusButton.setOnClickListener(v -> onIncrement.accept(line.menuItemId()));
        b.minusButton.setOnClickListener(v -> onDecrement.accept(line.menuItemId()));
    }
}
