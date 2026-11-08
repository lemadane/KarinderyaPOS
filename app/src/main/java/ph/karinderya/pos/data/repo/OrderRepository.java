package ph.karinderya.pos.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import ph.karinderya.pos.data.db.CursorReader;
import ph.karinderya.pos.data.db.DbHelper;
import ph.karinderya.pos.data.db.Schema;
import ph.karinderya.pos.data.model.CartLine;
import ph.karinderya.pos.data.model.DiscountType;
import ph.karinderya.pos.data.model.Order;
import ph.karinderya.pos.data.model.OrderDraft;
import ph.karinderya.pos.data.model.OrderFilter;
import ph.karinderya.pos.data.model.OrderItem;
import ph.karinderya.pos.data.model.OrderStatus;
import ph.karinderya.pos.data.model.OrderType;
import ph.karinderya.pos.data.model.PaymentMethod;
import ph.karinderya.pos.data.model.Totals;
import ph.karinderya.pos.data.model.User;
import ph.karinderya.pos.util.DateRange;

public record OrderRepository(DbHelper db) {

     static final int HISTORY_LIMIT = 500;
     static final int SQL_IN_CHUNK = 500;
     static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyMMdd", Locale.US);


    public Order create(OrderDraft draft, User crew) {
        if (draft.lines().isEmpty()) {
            throw new IllegalArgumentException("Order has no items");
        }
        Totals totals = Totals.of(draft.lines(), draft.discountPercent());
        long tendered = draft.cashTenderedCents();
        if (draft.paymentMethod() == PaymentMethod.CASH) {
            if (tendered < totals.totalCents()) {
                throw new IllegalArgumentException("Cash tendered is less than the total");
            }
        } else {
            tendered = totals.totalCents();
        }
        Instant now = Instant.now();
        UUID orderId = UUID.randomUUID();
        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();
        try {
            List<OrderItem> items = new ArrayList<>();
            for (CartLine line : draft.lines()) {
                items.add(new OrderItem(UUID.randomUUID(), orderId, line.menuItemId(), line.name(),
                        line.unitPriceCents(), line.quantity(), line.lineTotalCents()));
            }
            Order order = new Order(orderId, nextOrderNo(database, now), draft.orderType(),
                    blankToNull(draft.customerLabel()), OrderStatus.PREPARING, draft.paymentMethod(),
                    draft.discountType(), totals.subtotalCents(), totals.discountCents(), totals.totalCents(),
                    tendered, tendered - totals.totalCents(), blankToNull(draft.notes()),
                    crew.id(), crew.fullName(), now, now, items, draft.discountName(), draft.discountPercent());
            insert(database, order);
            database.setTransactionSuccessful();
            return order;
        } finally {
            database.endTransaction();
        }
    }

    /** Insert-or-replace keyed on UUID; used by backup import. */
    public void upsert(Order order) {
        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();
        try {
            database.delete(Schema.ORDER_ITEMS, "order_id = ?", new String[]{order.id().toString()});
            database.insertWithOnConflict(Schema.ORDERS, null, toValues(order), SQLiteDatabase.CONFLICT_REPLACE);
            for (OrderItem item : order.items()) {
                database.insertWithOnConflict(Schema.ORDER_ITEMS, null, toValues(item),
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public List<Order> list(OrderFilter filter, String query) {
        StringBuilder where = new StringBuilder("1 = 1");
        List<String> args = new ArrayList<>();
        String orderBy = "created_at DESC";
        switch (filter) {
            case ACTIVE -> {
                where.append(" AND status IN (?, ?)");
                args.add(OrderStatus.PREPARING.name());
                args.add(OrderStatus.READY.name());
                orderBy = "created_at ASC";
            }
            case TODAY -> {
                DateRange today = DateRange.today();
                where.append(" AND created_at >= ? AND created_at < ?");
                args.add(String.valueOf(today.from().toEpochMilli()));
                args.add(String.valueOf(today.to().toEpochMilli()));
            }
            case ALL -> {
            }
        }
        if (query != null && !query.isBlank()) {
            where.append(" AND (order_no LIKE ? OR customer_label LIKE ?)");
            String like = "%" + query.trim() + "%";
            args.add(like);
            args.add(like);
        }
        List<Order> orders = new ArrayList<>();
        try (Cursor c = db.getReadableDatabase().query(Schema.ORDERS, null, where.toString(),
                args.toArray(new String[0]), null, null, orderBy, String.valueOf(HISTORY_LIMIT))) {
            CursorReader r = new CursorReader(c);
            while (c.moveToNext()) {
                orders.add(map(r));
            }
        }
        return attachItems(orders);
    }

    /** Every order with items, oldest first; used by backup export. */
    public List<Order> listAllForBackup() {
        List<Order> orders = new ArrayList<>();
        try (Cursor c = db.getReadableDatabase().query(Schema.ORDERS, null, null, null, null, null,
                "created_at ASC")) {
            CursorReader r = new CursorReader(c);
            while (c.moveToNext()) {
                orders.add(map(r));
            }
        }
        return attachItems(orders);
    }

    public Optional<Order> findById(UUID id) {
        try (Cursor c = db.getReadableDatabase().query(Schema.ORDERS, null, "id = ?",
                new String[]{id.toString()}, null, null, null)) {
            if (!c.moveToFirst()) {
                return Optional.empty();
            }
            return Optional.of(attachItems(List.of(map(new CursorReader(c)))).get(0));
        }
    }

    public void updateStatus(UUID id, OrderStatus status) {
        ContentValues values = new ContentValues();
        values.put("status", status.name());
        values.put("updated_at", Instant.now().toEpochMilli());
        db.getWritableDatabase().update(Schema.ORDERS, values, "id = ?", new String[]{id.toString()});
    }

    public int countActive() {
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM orders WHERE status IN (?, ?)",
                new String[]{OrderStatus.PREPARING.name(), OrderStatus.READY.name()})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /** Human-friendly daily sequence such as {@code 261019-007}; the UUID remains the real key. */
     static String nextOrderNo(SQLiteDatabase database, Instant now) {
        ZoneId zone = ZoneId.systemDefault();
        String prefix = ORDER_NO_DATE.format(now.atZone(zone)) + "-";
        int next = 1;
        try (Cursor c = database.rawQuery(
                "SELECT order_no FROM orders WHERE order_no LIKE ? ORDER BY order_no DESC LIMIT 1",
                new String[]{prefix + "%"})) {
            if (c.moveToFirst()) {
                try {
                    next = Integer.parseInt(c.getString(0).substring(prefix.length())) + 1;
                } catch (NumberFormatException ignored) {
                    next = 1;
                }
            }
        }
        return prefix + String.format(Locale.US, "%03d", next);
    }

     List<Order> attachItems(List<Order> orders) {
        if (orders.isEmpty()) {
            return orders;
        }
        Map<UUID, List<OrderItem>> byOrder = new HashMap<>();
        SQLiteDatabase database = db.getReadableDatabase();
        for (int start = 0; start < orders.size(); start += SQL_IN_CHUNK) {
            List<Order> chunk = orders.subList(start, Math.min(orders.size(), start + SQL_IN_CHUNK));
            String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
            String[] ids = chunk.stream().map(o -> o.id().toString()).toArray(String[]::new);
            try (Cursor c = database.query(Schema.ORDER_ITEMS, null, "order_id IN (" + placeholders + ")", ids,
                    null, null, "rowid ASC")) {
                CursorReader r = new CursorReader(c);
                while (c.moveToNext()) {
                    OrderItem item = mapItem(r);
                    byOrder.computeIfAbsent(item.orderId(), k -> new ArrayList<>()).add(item);
                }
            }
        }
        List<Order> result = new ArrayList<>(orders.size());
        for (Order order : orders) {
            result.add(order.withItems(byOrder.getOrDefault(order.id(), List.of())));
        }
        return result;
    }

     static void insert(SQLiteDatabase database, Order order) {
        database.insertOrThrow(Schema.ORDERS, null, toValues(order));
        for (OrderItem item : order.items()) {
            database.insertOrThrow(Schema.ORDER_ITEMS, null, toValues(item));
        }
    }

     static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

     static ContentValues toValues(Order o) {
        ContentValues v = new ContentValues();
        v.put("id", o.id().toString());
        v.put("order_no", o.orderNo());
        v.put("order_type", o.orderType().name());
        v.put("customer_label", o.customerLabel());
        v.put("status", o.status().name());
        v.put("payment_method", o.paymentMethod().name());
        v.put("discount_type", o.discountType().name());
        v.put("discount_name", o.discountName());
        v.put("discount_percent", o.discountPercent());
        v.put("subtotal_cents", o.subtotalCents());
        v.put("discount_cents", o.discountCents());
        v.put("total_cents", o.totalCents());
        v.put("cash_tendered_cents", o.cashTenderedCents());
        v.put("change_cents", o.changeCents());
        v.put("notes", o.notes());
        v.put("created_by_user_id", o.createdByUserId().toString());
        v.put("created_by_name", o.createdByName());
        v.put("created_at", o.createdAt().toEpochMilli());
        v.put("updated_at", o.updatedAt().toEpochMilli());
        return v;
    }

     static ContentValues toValues(OrderItem i) {
        ContentValues v = new ContentValues();
        v.put("id", i.id().toString());
        v.put("order_id", i.orderId().toString());
        v.put("menu_item_id", i.menuItemId() == null ? null : i.menuItemId().toString());
        v.put("name", i.name());
        v.put("unit_price_cents", i.unitPriceCents());
        v.put("quantity", i.quantity());
        v.put("line_total_cents", i.lineTotalCents());
        return v;
    }

     static Order map(CursorReader r) {
        return new Order(r.uuid("id"), r.string("order_no"),
                r.enumValue("order_type", OrderType.class, OrderType.DINE_IN),
                r.string("customer_label"),
                r.enumValue("status", OrderStatus.class, OrderStatus.PREPARING),
                r.enumValue("payment_method", PaymentMethod.class, PaymentMethod.CASH),
                r.enumValue("discount_type", DiscountType.class, DiscountType.NONE),
                r.getLong("subtotal_cents"), r.getLong("discount_cents"), r.getLong("total_cents"),
                r.getLong("cash_tendered_cents"), r.getLong("change_cents"), r.string("notes"),
                r.uuid("created_by_user_id"), r.string("created_by_name"),
                r.instant("created_at"), r.instant("updated_at"), List.of(),
                r.string("discount_name"), r.getInt("discount_percent"));
    }

     static OrderItem mapItem(CursorReader r) {
        return new OrderItem(r.uuid("id"), r.uuid("order_id"), r.uuid("menu_item_id"), r.string("name"),
                r.getLong("unit_price_cents"), r.getInt("quantity"), r.getLong("line_total_cents"));
    }
}
