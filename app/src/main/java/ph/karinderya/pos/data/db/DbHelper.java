package ph.karinderya.pos.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.UUID;

public final class DbHelper extends SQLiteOpenHelper {

     static final String DB_NAME = "karinderya_pos.db";
     static final int DB_VERSION = 5;

    public DbHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Schema.CREATE_USERS);
        db.execSQL(Schema.CREATE_MENU_ITEMS);
        db.execSQL(Schema.CREATE_ORDERS);
        db.execSQL(Schema.CREATE_ORDER_ITEMS);
        db.execSQL(Schema.CREATE_DISCOUNTS);
        for (String index : Schema.INDEXES) {
            db.execSQL(index);
        }
        seedMenu(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Category set changed to Kanin, Ulam, Gulay, Drinks, Dessert, Mirienda.
            db.execSQL("UPDATE menu_items SET category = 'Mirienda'"
                    + " WHERE category IN ('Merienda', 'Miryenda')");
            db.execSQL("UPDATE menu_items SET category = 'Gulay'"
                    + " WHERE category = 'Ulam' AND name IN ('Pinakbet', 'Ginisang Monggo')");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE menu_items ADD COLUMN meal_of_day INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) {
            db.execSQL(Schema.CREATE_DISCOUNTS);
            db.execSQL("ALTER TABLE orders ADD COLUMN discount_name TEXT");
            db.execSQL("ALTER TABLE orders ADD COLUMN discount_percent INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE orders SET discount_percent = 20 WHERE discount_type IN ('SENIOR', 'PWD')");
        }
        if (oldVersion >= 4 && oldVersion < 5) {
            db.execSQL("ALTER TABLE discounts RENAME TO discounts_v4");
            db.execSQL(Schema.CREATE_DISCOUNTS);
            db.execSQL("INSERT INTO discounts (id, name, percent) SELECT id, name, percent FROM discounts_v4");
            db.execSQL("DROP TABLE discounts_v4");
        }
    }

    /** Sample karinderya menu so a fresh install is usable right away; crew can edit or delete these. */
     static void seedMenu(SQLiteDatabase db) {
        Object[][] items = {
                {"Adobong Manok", "Chicken stewed in soy sauce, vinegar and garlic", "Ulam", 8000L},
                {"Sinigang na Baboy", "Pork in sour tamarind soup with vegetables", "Ulam", 9000L},
                {"Pinakbet", "Mixed vegetables with bagoong", "Gulay", 6000L},
                {"Ginisang Monggo", "Sauteed mung beans", "Gulay", 5000L},
                {"Pritong Isda", "Fried fish of the day", "Ulam", 7000L},
                {"Kanin", "One cup steamed rice", "Kanin", 1500L},
                {"Sinangag", "Garlic fried rice", "Kanin", 2000L},
                {"Turon", "Banana and langka spring roll", "Mirienda", 2000L},
                {"Pancit Canton", "Stir-fried egg noodles", "Mirienda", 4500L},
                {"Softdrinks (Mismo)", "Assorted flavors", "Drinks", 2500L},
                {"Gulaman", "Sago't gulaman", "Drinks", 2000L},
                {"Leche Flan", "Caramel custard", "Dessert", 3500L}
        };
        long now = System.currentTimeMillis();
        for (Object[] item : items) {
            ContentValues values = new ContentValues();
            values.put("id", UUID.randomUUID().toString());
            values.put("name", (String) item[0]);
            values.put("description", (String) item[1]);
            values.put("category", (String) item[2]);
            values.put("price_cents", (Long) item[3]);
            values.put("available", 1);
            values.put("created_at", now);
            values.put("updated_at", now);
            db.insertOrThrow(Schema.MENU_ITEMS, null, values);
        }
    }
}
