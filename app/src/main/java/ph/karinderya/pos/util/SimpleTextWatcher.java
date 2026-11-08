package ph.karinderya.pos.util;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.function.Consumer;

public final class SimpleTextWatcher implements TextWatcher {

     final Consumer<String> onChanged;

    public SimpleTextWatcher(Consumer<String> onChanged) {
        this.onChanged = onChanged;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        onChanged.accept(s.toString());
    }
}
