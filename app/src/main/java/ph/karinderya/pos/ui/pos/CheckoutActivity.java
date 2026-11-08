package ph.karinderya.pos.ui.pos;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.ArrayList;

import ph.karinderya.pos.R;
import ph.karinderya.pos.data.CartManager;
import ph.karinderya.pos.data.model.CartLine;
import ph.karinderya.pos.data.model.CustomDiscount;
import ph.karinderya.pos.data.model.DiscountType;
import ph.karinderya.pos.data.model.OrderDraft;
import ph.karinderya.pos.data.model.OrderType;
import ph.karinderya.pos.data.model.PaymentMethod;
import ph.karinderya.pos.data.model.Totals;
import ph.karinderya.pos.databinding.ActivityCheckoutBinding;
import ph.karinderya.pos.ui.common.AuthedActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.ui.orders.OrderDetailActivity;
import ph.karinderya.pos.util.Async;
import ph.karinderya.pos.util.Money;
import ph.karinderya.pos.util.SimpleTextWatcher;

public final class CheckoutActivity extends AuthedActivity {

     static final long[] QUICK_CASH_PESOS = {20, 50, 100, 200, 500, 1000};

     ActivityCheckoutBinding binding;
     CartManager cart;
     CartLineAdapter adapter;
     boolean submitting;
     boolean discountsLoaded;
     DiscountType selectedDiscountType = DiscountType.NONE;
     CustomDiscount selectedCustomDiscount;
     final Runnable cartListener = this::onCartChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sessionValid) {
            return;
        }
        binding = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.applyInsets(binding.root, binding.toolbar);
        setupToolbar(binding.toolbar, getString(R.string.checkout_title), true);
        cart = app.cart();

        adapter = new CartLineAdapter(cart::increment, cart::decrement);
        binding.cartList.setLayoutManager(new LinearLayoutManager(this));
        binding.cartList.setAdapter(adapter);

        if (savedInstanceState == null) {
            binding.orderTypeGroup.check(R.id.dineInButton);
            binding.paymentGroup.check(R.id.cashButton);
        }
        binding.orderTypeGroup.addOnButtonCheckedListener((g, id, checked) -> recalc());
        binding.paymentGroup.addOnButtonCheckedListener((g, id, checked) -> recalc());
        binding.cashInput.addTextChangedListener(new SimpleTextWatcher(t -> recalc()));
        buildQuickCash();
        binding.placeOrderButton.setOnClickListener(v -> placeOrder());

        cart.addListener(cartListener);
        onCartChanged();
        loadDiscounts(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selected_discount", selectedCustomDiscount == null
                ? selectedDiscountType.name() : selectedCustomDiscount.id().toString());
    }

     void loadDiscounts(Bundle savedInstanceState) {
        binding.discountInput.setEnabled(false);
        Async.run(() -> app.discounts().list(), discounts -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            List<String> labels = new ArrayList<>(List.of(getString(R.string.discount_none),
                    getString(R.string.discount_senior), getString(R.string.discount_pwd)));
            for (CustomDiscount discount : discounts) {
                labels.add(getString(R.string.discount_option, discount.name(), discount.percent()));
            }
            String savedKey = savedInstanceState == null ? DiscountType.NONE.name()
                    : savedInstanceState.getString("selected_discount", DiscountType.NONE.name());
            int selection = 0;
            if (DiscountType.SENIOR.name().equals(savedKey)) {
                selection = 1;
            } else if (DiscountType.PWD.name().equals(savedKey)) {
                selection = 2;
            } else if (!DiscountType.NONE.name().equals(savedKey)) {
                for (int i = 0; i < discounts.size(); i++) {
                    if (discounts.get(i).id().toString().equals(savedKey)) {
                        selection = i + 3;
                        break;
                    }
                }
                if (selection == 0) {
                    Ui.toast(this, getString(R.string.discount_unavailable));
                }
            }
            selectDiscount(selection, discounts);
            binding.discountInput.setSimpleItems(labels.toArray(new String[0]));
            binding.discountInput.setText(labels.get(selection), false);
            binding.discountInput.setOnItemClickListener((parent, view, position, id) -> {
                selectDiscount(position, discounts);
                recalc();
            });
            discountsLoaded = true;
            binding.discountInput.setEnabled(true);
            recalc();
        }, e -> Ui.showError(this, e));
    }

     void selectDiscount(int position, List<CustomDiscount> discounts) {
        selectedCustomDiscount = position >= 3 ? discounts.get(position - 3) : null;
        selectedDiscountType = switch (position) {
            case 0 -> DiscountType.NONE;
            case 1 -> DiscountType.SENIOR;
            case 2 -> DiscountType.PWD;
            default -> DiscountType.CUSTOM;
        };
    }

    @Override
    protected void onDestroy() {
        if (cart != null) {
            cart.removeListener(cartListener);
        }
        super.onDestroy();
    }

     void buildQuickCash() {
        addQuickCashChip(getString(R.string.exact), -1);
        for (long pesos : QUICK_CASH_PESOS) {
            addQuickCashChip(Money.format(pesos * 100).replace(".00", ""), pesos * 100);
        }
    }

     void addQuickCashChip(String label, long cents) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_action, binding.quickCashChips, false);
        chip.setText(label);
        chip.setOnClickListener(v -> {
            long amount = cents < 0 ? currentTotals().totalCents() : cents;
            binding.cashInput.setText(Money.toPlain(amount));
            binding.cashInput.setSelection(binding.cashInput.length());
        });
        binding.quickCashChips.addView(chip);
    }

     void onCartChanged() {
        if (cart.isEmpty() && !submitting) {
            finish();
            return;
        }
        adapter.submitList(cart.lines());
        recalc();
    }

     DiscountType discountType() {
        return selectedDiscountType;
    }

     PaymentMethod paymentMethod() {
        int id = binding.paymentGroup.getCheckedButtonId();
        if (id == R.id.gcashButton) {
            return PaymentMethod.GCASH;
        }
        return id == R.id.mayaButton ? PaymentMethod.MAYA : PaymentMethod.CASH;
    }

     OrderType orderType() {
        return binding.orderTypeGroup.getCheckedButtonId() == R.id.takeOutButton ? OrderType.TAKE_OUT
                : OrderType.DINE_IN;
    }

     Totals currentTotals() {
        return Totals.of(cart.lines(), selectedCustomDiscount == null
                ? discountType().percent() : selectedCustomDiscount.percent());
    }

     long cashTendered() {
        return Money.parseOrZero(String.valueOf(binding.cashInput.getText()));
    }

     void recalc() {
        Totals totals = currentTotals();
        boolean cash = paymentMethod() == PaymentMethod.CASH;
        long tendered = cashTendered();
        binding.cashSection.setVisibility(cash ? View.VISIBLE : View.GONE);
        binding.subtotalValue.setText(Money.format(totals.subtotalCents()));
        binding.discountRow.setVisibility(totals.discountCents() > 0 ? View.VISIBLE : View.GONE);
        binding.discountValue.setText(getString(R.string.negative_amount, Money.format(totals.discountCents())));
        binding.totalValue.setText(Money.format(totals.totalCents()));
        binding.changeRow.setVisibility(cash ? View.VISIBLE : View.GONE);
        boolean shortCash = cash && tendered < totals.totalCents();
        binding.changeValue.setText(shortCash ? getString(R.string.no_value)
                : Money.format(tendered - totals.totalCents()));
        binding.cashLayout.setError(cash && tendered > 0 && shortCash ? getString(R.string.error_cash_short) : null);
        binding.placeOrderButton.setText(getString(R.string.place_order, Money.format(totals.totalCents())));
        binding.placeOrderButton.setEnabled(discountsLoaded && !submitting && !cart.isEmpty() && !shortCash);
    }

     void placeOrder() {
        if (submitting || !discountsLoaded) {
            return;
        }
        List<CartLine> lines = cart.lines();
        if (lines.isEmpty()) {
            Ui.toast(this, getString(R.string.cart_empty));
            return;
        }
        OrderDraft draft = new OrderDraft(orderType(), String.valueOf(binding.customerInput.getText()),
                discountType(), paymentMethod(), cashTendered(), String.valueOf(binding.notesInput.getText()),
                lines, selectedCustomDiscount);
        submitting = true;
        recalc();
        Async.run(() -> app.orders().create(draft, app.session().requireUser()), order -> {
            cart.clear();
            Ui.toast(this, getString(R.string.order_placed, order.orderNo()));
            startActivity(new Intent(this, OrderDetailActivity.class)
                    .putExtra(OrderDetailActivity.EXTRA_ORDER_ID, order.id().toString()));
            finish();
        }, e -> {
            submitting = false;
            recalc();
            Ui.showError(this, e);
        });
    }
}
