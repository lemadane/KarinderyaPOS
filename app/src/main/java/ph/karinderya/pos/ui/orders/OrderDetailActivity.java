package ph.karinderya.pos.ui.orders;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.UUID;

import ph.karinderya.pos.R;
import ph.karinderya.pos.data.model.Order;
import ph.karinderya.pos.data.model.OrderStatus;
import ph.karinderya.pos.databinding.ActivityOrderDetailBinding;
import ph.karinderya.pos.ui.common.AuthedActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Async;
import ph.karinderya.pos.util.ReceiptFormatter;

public final class OrderDetailActivity extends AuthedActivity {

    public static final String EXTRA_ORDER_ID = "order_id";

     ActivityOrderDetailBinding binding;
     UUID orderId;
     Order order;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sessionValid) {
            return;
        }
        binding = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.applyInsets(binding.root, binding.toolbar);
        setupToolbar(binding.toolbar, "", true);
        try {
            orderId = UUID.fromString(getIntent().getStringExtra(EXTRA_ORDER_ID));
        } catch (IllegalArgumentException | NullPointerException e) {
            finish();
            return;
        }
        binding.advanceButton.setOnClickListener(v -> {
            OrderStatus next = order == null ? null : order.status().next();
            if (next != null) {
                updateStatus(next);
            }
        });
        binding.cancelButton.setOnClickListener(v -> Ui.confirm(this, getString(R.string.cancel_order_confirm),
                () -> updateStatus(OrderStatus.CANCELLED)));
        load();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_order_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_share && order != null) {
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.order_title, order.orderNo()))
                    .putExtra(Intent.EXTRA_TEXT, receipt());
            startActivity(Intent.createChooser(send, getString(R.string.share_receipt)));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

     void load() {
        Async.run(() -> app.orders().findById(orderId), result -> {
            if (result.isEmpty()) {
                finish();
                return;
            }
            order = result.get();
            render();
        }, e -> Ui.showError(this, e));
    }

     void updateStatus(OrderStatus status) {
        binding.advanceButton.setEnabled(false);
        binding.cancelButton.setEnabled(false);
        Async.run(() -> {
            app.orders().updateStatus(orderId, status);
            return true;
        }, ok -> load(), e -> {
            render();
            Ui.showError(this, e);
        });
    }

     String receipt() {
        return ReceiptFormatter.format(order, app.settings().storeName());
    }

     void render() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.order_title, order.orderNo()));
        }
        binding.status.setText(order.status().label());
        binding.status.setBackgroundColor(Ui.statusColor(this, order.status()));
        binding.receiptText.setText(receipt());
        OrderStatus next = order.status().next();
        binding.actions.setVisibility(order.status().isActive() ? View.VISIBLE : View.GONE);
        binding.advanceButton.setEnabled(true);
        binding.cancelButton.setEnabled(true);
        if (next != null) {
            binding.advanceButton.setText(getString(R.string.mark_status, next.label()));
        }
    }
}
