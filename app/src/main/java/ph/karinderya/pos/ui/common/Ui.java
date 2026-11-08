package ph.karinderya.pos.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import ph.karinderya.pos.R;
import ph.karinderya.pos.data.model.OrderStatus;

public final class Ui {

     Ui() {
    }

    /** Draws behind system bars; {@code lightIcons} for screens whose top is the red toolbar. */
    public static void enableEdgeToEdge(ComponentActivity activity, boolean lightIcons) {
        SystemBarStyle status = lightIcons
                ? SystemBarStyle.dark(Color.TRANSPARENT)
                : SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT);
        EdgeToEdge.enable(activity, status, SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT));
    }

    /**
     * Pads {@code root} for side/bottom bars and the keyboard. The status bar inset goes to
     * {@code topView} (usually the toolbar, so its colour extends under the status bar) or to root.
     */
    public static void applyInsets(View root, View topView) {
        int topBase = topView == null ? 0 : topView.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, topView == null ? bars.top : 0, bars.right, Math.max(bars.bottom, ime.bottom));
            if (topView != null) {
                topView.setPadding(topView.getPaddingLeft(), topBase + bars.top, topView.getPaddingRight(),
                        topView.getPaddingBottom());
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    public static void toast(Context context, CharSequence message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showError(Context context, Throwable error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        if (error instanceof IllegalArgumentException) {
            toast(context, message);
        } else {
            toast(context, context.getString(R.string.error_generic, message));
        }
    }

    public static void confirm(Context context, CharSequence message, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(context)
                .setMessage(message)
                .setPositiveButton(R.string.yes, (d, w) -> onConfirm.run())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    public static int statusColor(Context context, OrderStatus status) {
        int res = switch (status) {
            case PREPARING -> R.color.status_preparing;
            case READY -> R.color.status_ready;
            case SERVED -> R.color.status_served;
            case CANCELLED -> R.color.status_cancelled;
        };
        return ContextCompat.getColor(context, res);
    }
}
