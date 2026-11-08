package ph.karinderya.pos.ui.common;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public final class BindingViewHolder<VB extends ViewBinding> extends RecyclerView.ViewHolder {

    public final VB binding;

    public BindingViewHolder(@NonNull VB binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
