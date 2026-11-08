package ph.karinderya.pos.data.repo;

import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ph.karinderya.pos.data.db.CursorReader;
import ph.karinderya.pos.data.db.DbHelper;
import ph.karinderya.pos.data.db.Schema;
import ph.karinderya.pos.data.model.CustomDiscount;

public record DiscountRepository(DbHelper db) {

    public List<CustomDiscount> list() {
        List<CustomDiscount> discounts = new ArrayList<>();
        try (var cursor = db.getReadableDatabase().query(Schema.DISCOUNTS, null, null, null,
                null, null, "name COLLATE NOCASE, id")) {
            CursorReader reader = new CursorReader(cursor);
            while (cursor.moveToNext()) {
                discounts.add(new CustomDiscount(reader.uuid("id"), reader.string("name"), reader.getInt("percent")));
            }
        }
        return discounts;
    }

    public void save(CustomDiscount discount) {
        ContentValues values = new ContentValues();
        values.put("id", discount.id().toString());
        values.put("name", discount.name());
        values.put("percent", discount.percent());
        var database = db.getWritableDatabase();
        database.beginTransaction();
        try {
            int updated = database.update(Schema.DISCOUNTS, values, "id = ?",
                    new String[]{discount.id().toString()});
            if (updated == 0) {
                database.insertOrThrow(Schema.DISCOUNTS, null, values);
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public void delete(UUID id) {
        if (db.getWritableDatabase().delete(Schema.DISCOUNTS, "id = ?", new String[]{id.toString()}) != 1) {
            throw new IllegalArgumentException("Discount no longer exists");
        }
    }
}
