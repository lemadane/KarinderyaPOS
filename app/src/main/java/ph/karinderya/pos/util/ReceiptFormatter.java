package ph.karinderya.pos.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import ph.karinderya.pos.data.model.DiscountType;
import ph.karinderya.pos.data.model.Order;
import ph.karinderya.pos.data.model.OrderItem;
import ph.karinderya.pos.data.model.PaymentMethod;

/** Plain-text, 32-column receipt (fits common 58mm thermal printers). */
public final class ReceiptFormatter {

    public static final int WIDTH = 32;
     static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US);

     ReceiptFormatter() {
    }

    public static String format(Order order, String storeName) {
        StringBuilder sb = new StringBuilder();
        center(sb, storeName);
        center(sb, "ORDER SLIP");
        line(sb);
        sb.append("Order #: ").append(order.orderNo()).append('\n');
        sb.append("Date   : ").append(formatDateTime(order.createdAt())).append('\n');
        sb.append("Type   : ").append(order.orderType().label()).append('\n');
        if (order.customerLabel() != null && !order.customerLabel().isBlank()) {
            sb.append("For    : ").append(order.customerLabel()).append('\n');
        }
        sb.append("Crew   : ").append(order.createdByName()).append('\n');
        line(sb);
        for (OrderItem item : order.items()) {
            sb.append(item.name()).append('\n');
            String qty = "  " + item.quantity() + " x " + Money.format(item.unitPriceCents());
            row(sb, qty, Money.format(item.lineTotalCents()));
        }
        line(sb);
        row(sb, "Subtotal", Money.format(order.subtotalCents()));
        if (order.discountType() != DiscountType.NONE) {
            row(sb, order.discountName() + " " + order.discountPercent() + "%",
                    "-" + Money.format(order.discountCents()));
        }
        row(sb, "TOTAL", Money.format(order.totalCents()));
        row(sb, "Paid via", order.paymentMethod().label());
        if (order.paymentMethod() == PaymentMethod.CASH) {
            row(sb, "Cash", Money.format(order.cashTenderedCents()));
            row(sb, "Change", Money.format(order.changeCents()));
        }
        if (order.notes() != null && !order.notes().isBlank()) {
            line(sb);
            sb.append("Notes: ").append(order.notes()).append('\n');
        }
        line(sb);
        center(sb, "Salamat po! Balik po kayo!");
        return sb.toString();
    }

    public static String formatDateTime(Instant instant) {
        return DATE_TIME.format(instant.atZone(ZoneId.systemDefault()));
    }

     static void row(StringBuilder sb, String left, String right) {
        int spaces = WIDTH - left.length() - right.length();
        if (spaces < 1) {
            sb.append(left).append('\n');
            spaces = WIDTH - right.length();
            left = "";
        }
        sb.append(left).append(" ".repeat(Math.max(1, spaces))).append(right).append('\n');
    }

     static void center(StringBuilder sb, String text) {
        int pad = Math.max(0, (WIDTH - text.length()) / 2);
        sb.append(" ".repeat(pad)).append(text).append('\n');
    }

     static void line(StringBuilder sb) {
        sb.append("-".repeat(WIDTH)).append('\n');
    }
}
