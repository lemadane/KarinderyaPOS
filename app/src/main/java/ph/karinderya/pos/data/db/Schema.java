package ph.karinderya.pos.data.db;

/** Table and column names. All primary keys are UUID strings stored as TEXT. */
public final class Schema {

    public static final String USERS = "users";
    public static final String MENU_ITEMS = "menu_items";
    public static final String ORDERS = "orders";
    public static final String ORDER_ITEMS = "order_items";
    public static final String DISCOUNTS = "discounts";

    static final String CREATE_DISCOUNTS = """
            CREATE TABLE discounts (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                percent INTEGER NOT NULL CHECK (percent IN (5, 8, 10, 12, 15, 18, 20))
            )""";

    static final String CREATE_USERS = """
            CREATE TABLE users (
                id TEXT PRIMARY KEY NOT NULL,
                username TEXT NOT NULL UNIQUE COLLATE NOCASE,
                full_name TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )""";

    static final String CREATE_MENU_ITEMS = """
            CREATE TABLE menu_items (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                description TEXT,
                category TEXT NOT NULL,
                price_cents INTEGER NOT NULL CHECK (price_cents >= 0),
                photo_path TEXT,
                available INTEGER NOT NULL DEFAULT 1,
                meal_of_day INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )""";

    static final String CREATE_ORDERS = """
            CREATE TABLE orders (
                id TEXT PRIMARY KEY NOT NULL,
                order_no TEXT NOT NULL,
                order_type TEXT NOT NULL,
                customer_label TEXT,
                status TEXT NOT NULL,
                payment_method TEXT NOT NULL,
                discount_type TEXT NOT NULL,
                discount_name TEXT,
                discount_percent INTEGER NOT NULL DEFAULT 0,
                subtotal_cents INTEGER NOT NULL,
                discount_cents INTEGER NOT NULL,
                total_cents INTEGER NOT NULL,
                cash_tendered_cents INTEGER NOT NULL,
                change_cents INTEGER NOT NULL,
                notes TEXT,
                created_by_user_id TEXT NOT NULL,
                created_by_name TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )""";

    static final String CREATE_ORDER_ITEMS = """
            CREATE TABLE order_items (
                id TEXT PRIMARY KEY NOT NULL,
                order_id TEXT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                menu_item_id TEXT,
                name TEXT NOT NULL,
                unit_price_cents INTEGER NOT NULL,
                quantity INTEGER NOT NULL CHECK (quantity > 0),
                line_total_cents INTEGER NOT NULL
            )""";

    static final String[] INDEXES = {
            "CREATE INDEX idx_menu_category ON menu_items(category)",
            "CREATE INDEX idx_orders_created ON orders(created_at)",
            "CREATE INDEX idx_orders_status ON orders(status)",
            "CREATE INDEX idx_order_items_order ON order_items(order_id)"
    };

     Schema() {
    }
}
