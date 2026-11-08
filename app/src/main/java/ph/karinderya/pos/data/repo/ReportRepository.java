package ph.karinderya.pos.data.repo;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ph.karinderya.pos.data.db.DbHelper;
import ph.karinderya.pos.data.model.OrderStatus;
import ph.karinderya.pos.data.model.PaymentMethod;
import ph.karinderya.pos.data.model.SalesSummary;
import ph.karinderya.pos.data.model.TopItem;
import ph.karinderya.pos.util.DateRange;

public record ReportRepository(DbHelper db) {

     static final int TOP_ITEMS = 10;


    /** Sales for non-cancelled orders within the range. */
    public SalesSummary summary(DateRange range) {
        SQLiteDatabase database = db.getReadableDatabase();
        String from = String.valueOf(range.from().toEpochMilli());
        String to = String.valueOf(range.to().toEpochMilli());
        String cancelled = OrderStatus.CANCELLED.name();

        int orderCount = 0;
        long gross = 0;
        long discount = 0;
        long net = 0;
        try (Cursor c = database.rawQuery("""
                SELECT COUNT(*), COALESCE(SUM(subtotal_cents), 0), COALESCE(SUM(discount_cents), 0),
                       COALESCE(SUM(total_cents), 0)
                FROM orders WHERE created_at >= ? AND created_at < ? AND status != ?""",
                new String[]{from, to, cancelled})) {
            if (c.moveToFirst()) {
                orderCount = c.getInt(0);
                gross = c.getLong(1);
                discount = c.getLong(2);
                net = c.getLong(3);
            }
        }

        int cancelledCount = 0;
        try (Cursor c = database.rawQuery(
                "SELECT COUNT(*) FROM orders WHERE created_at >= ? AND created_at < ? AND status = ?",
                new String[]{from, to, cancelled})) {
            if (c.moveToFirst()) {
                cancelledCount = c.getInt(0);
            }
        }

        Map<PaymentMethod, Long> byPayment = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            byPayment.put(method, 0L);
        }
        try (Cursor c = database.rawQuery("""
                SELECT payment_method, SUM(total_cents) FROM orders
                WHERE created_at >= ? AND created_at < ? AND status != ?
                GROUP BY payment_method""", new String[]{from, to, cancelled})) {
            while (c.moveToNext()) {
                try {
                    byPayment.put(PaymentMethod.valueOf(c.getString(0)), c.getLong(1));
                } catch (IllegalArgumentException ignored) {
                    // Unknown method from a newer backup; skip it.
                }
            }
        }

        List<TopItem> topItems = new ArrayList<>();
        try (Cursor c = database.rawQuery("""
                SELECT oi.name, SUM(oi.quantity) AS qty, SUM(oi.line_total_cents)
                FROM order_items oi JOIN orders o ON o.id = oi.order_id
                WHERE o.created_at >= ? AND o.created_at < ? AND o.status != ?
                GROUP BY oi.name ORDER BY qty DESC, oi.name LIMIT ?""",
                new String[]{from, to, cancelled, String.valueOf(TOP_ITEMS)})) {
            while (c.moveToNext()) {
                topItems.add(new TopItem(c.getString(0), c.getInt(1), c.getLong(2)));
            }
        }
        return new SalesSummary(orderCount, cancelledCount, gross, discount, net, byPayment, topItems);
    }
}
