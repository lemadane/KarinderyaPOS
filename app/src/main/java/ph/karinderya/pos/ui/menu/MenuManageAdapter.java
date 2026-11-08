package ph.karinderya.pos.ui.menu;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import ph.karinderya.pos.data.model.MenuItem;
import ph.karinderya.pos.databinding.ItemMenuManageBinding;
import ph.karinderya.pos.ui.common.BindingViewHolder;
import ph.karinderya.pos.ui.common.KeyDiffCallback;
import ph.karinderya.pos.util.ImageLoader;
import ph.karinderya.pos.util.Money;

public final class MenuManageAdapter extends ListAdapter<MenuItem, BindingViewHolder<ItemMenuManageBinding>> {

     final Consumer<MenuItem> onClick;
     final BiConsumer<MenuItem, Boolean> onAvailabilityChanged;

    public MenuManageAdapter(Consumer<MenuItem> onClick, BiConsumer<MenuItem, Boolean> onAvailabilityChanged) {
        super(new KeyDiffCallback<>(MenuItem::id));
        this.onClick = onClick;
        this.onAvailabilityChanged = onAvailabilityChanged;
    }

    @NonNull
    @Override
    public BindingViewHolder<ItemMenuManageBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BindingViewHolder<>(ItemMenuManageBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BindingViewHolder<ItemMenuManageBinding> holder, int position) {
        MenuItem item = getItem(position);
        ItemMenuManageBinding b = holder.binding;
        b.name.setText(item.name());
        b.category.setText(item.category());
        b.price.setText(Money.format(item.priceCents()));
        ImageLoader.load(b.photo, item.photoPath());
        b.availableSwitch.setOnCheckedChangeListener(null);
        b.availableSwitch.setChecked(item.available());
        b.availableSwitch.setOnCheckedChangeListener((v, checked) -> onAvailabilityChanged.accept(item, checked));
        b.getRoot().setOnClickListener(v -> onClick.accept(item));
    }
}
