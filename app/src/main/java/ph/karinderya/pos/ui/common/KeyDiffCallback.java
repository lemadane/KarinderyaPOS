package ph.karinderya.pos.ui.common;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.Objects;
import java.util.function.Function;

/** Diff callback for records: identity by key, contents by record {@code equals}. */
public final class KeyDiffCallback<T> extends DiffUtil.ItemCallback<T> {

     final Function<T, Object> key;

    public KeyDiffCallback(Function<T, Object> key) {
        this.key = key;
    }

    @Override
    public boolean areItemsTheSame(@NonNull T oldItem, @NonNull T newItem) {
        return Objects.equals(key.apply(oldItem), key.apply(newItem));
    }

    @Override
    public boolean areContentsTheSame(@NonNull T oldItem, @NonNull T newItem) {
        return Objects.equals(oldItem, newItem);
    }
}
