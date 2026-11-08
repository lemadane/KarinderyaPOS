package ph.karinderya.pos.ui.pos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

import ph.karinderya.pos.data.model.MenuItem;
import ph.karinderya.pos.databinding.ItemPosMenuBinding;
import ph.karinderya.pos.ui.common.BindingViewHolder;
import ph.karinderya.pos.ui.common.KeyDiffCallback;
import ph.karinderya.pos.util.ImageLoader;
import ph.karinderya.pos.util.Money;

public final class PosMenuAdapter extends ListAdapter<MenuItem, BindingViewHolder<ItemPosMenuBinding>> {

     final ToIntFunction<UUID> quantityInCart;
     final Consumer<MenuItem> onAdd;
     final Consumer<MenuItem> onRemoveOne;

    public PosMenuAdapter(ToIntFunction<UUID> quantityInCart, Consumer<MenuItem> onAdd,
                          Consumer<MenuItem> onRemoveOne) {
        super(new KeyDiffCallback<>(MenuItem::id));
        this.quantityInCart = quantityInCart;
        this.onAdd = onAdd;
        this.onRemoveOne = onRemoveOne;
    }

    @NonNull
    @Override
    public BindingViewHolder<ItemPosMenuBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BindingViewHolder<>(ItemPosMenuBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BindingViewHolder<ItemPosMenuBinding> holder, int position) {
        MenuItem item = getItem(position);
        ItemPosMenuBinding b = holder.binding;
        b.name.setText(item.name());
        b.price.setText(Money.format(item.priceCents()));
        ImageLoader.load(b.photo, item.photoPath());
        b.soldOut.setVisibility(item.available() ? View.GONE : View.VISIBLE);
        b.mealOfDayBadge.setVisibility(item.mealOfDay() ? View.VISIBLE : View.GONE);
        int qty = quantityInCart.applyAsInt(item.id());
        b.qtyBadge.setVisibility(qty > 0 ? View.VISIBLE : View.GONE);
        b.qtyBadge.setText(String.valueOf(qty));
        b.getRoot().setAlpha(item.available() ? 1f : 0.6f);
        b.getRoot().setOnClickListener(item.available() ? v -> onAdd.accept(item) : null);
        b.getRoot().setOnLongClickListener(qty > 0 ? v -> {
            onRemoveOne.accept(item);
            return true;
        } : null);
        b.getRoot().setClickable(item.available());
    }
}
