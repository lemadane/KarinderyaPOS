package ph.karinderya.pos.ui.more;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import java.util.Map;

import ph.karinderya.pos.R;
import ph.karinderya.pos.data.model.PaymentMethod;
import ph.karinderya.pos.data.model.SalesSummary;
import ph.karinderya.pos.data.model.TopItem;
import ph.karinderya.pos.databinding.ActivitySalesReportBinding;
import ph.karinderya.pos.databinding.ItemLabelValueBinding;
import ph.karinderya.pos.ui.common.AuthedActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Async;
import ph.karinderya.pos.util.DateRange;
import ph.karinderya.pos.util.Money;

public final class SalesReportActivity extends AuthedActivity {

     ActivitySalesReportBinding binding;
     int loadGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sessionValid) {
            return;
        }
        binding = ActivitySalesReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.applyInsets(binding.root, binding.toolbar);
        setupToolbar(binding.toolbar, getString(R.string.sales_report), true);
        binding.rangeChips.setOnCheckedStateChangeListener((group, ids) -> load());
        load();
    }

     DateRange range() {
        int id = binding.rangeChips.getCheckedChipId();
        if (id == R.id.chip7Days) {
            return DateRange.lastDays(7);
        }
        return id == R.id.chip30Days ? DateRange.lastDays(30) : DateRange.today();
    }

     void load() {
        DateRange range = range();
        int generation = ++loadGeneration;
        Async.run(() -> app.reports().summary(range), summary -> {
            if (generation == loadGeneration) {
                render(summary);
            }
        }, e -> Ui.showError(this, e));
    }

     void render(SalesSummary s) {
        binding.netSales.setText(Money.format(s.netCents()));
        binding.orderCount.setText(String.valueOf(s.orderCount()));
        binding.grossSales.setText(Money.format(s.grossCents()));
        binding.discountsTotal.setText(Money.format(s.discountCents()));
        binding.cancelledCount.setText(String.valueOf(s.cancelledCount()));

        binding.paymentContainer.removeAllViews();
        for (PaymentMethod method : PaymentMethod.values()) {
            long amount = s.byPayment().getOrDefault(method, 0L);
            addRow(binding.paymentContainer, method.label(), Money.format(amount));
        }

        binding.topItemsContainer.removeAllViews();
        if (s.topItems().isEmpty()) {
            addRow(binding.topItemsContainer, getString(R.string.no_sales), "");
        }
        int rank = 1;
        for (TopItem item : s.topItems()) {
            addRow(binding.topItemsContainer, rank++ + ". " + item.name() + " ×" + item.quantity(),
                    Money.format(item.totalCents()));
        }
    }

     void addRow(LinearLayout container, String label, String value) {
        ItemLabelValueBinding row = ItemLabelValueBinding.inflate(LayoutInflater.from(this), container, false);
        row.label.setText(label);
        row.value.setText(value);
        container.addView(row.getRoot());
    }
}
