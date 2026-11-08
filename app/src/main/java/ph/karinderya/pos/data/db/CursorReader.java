package ph.karinderya.pos.data.db;

import android.database.Cursor;

import java.time.Instant;
import java.util.UUID;

/** Null-safe, name-based column access. */
public final class CursorReader {

     final Cursor cursor;

    public CursorReader(Cursor cursor) {
        this.cursor = cursor;
    }

    public String string(String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getString(index);
    }

    public long getLong(String column) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(column));
    }

    public int getInt(String column) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(column));
    }

    public Instant instant(String column) {
        return Instant.ofEpochMilli(getLong(column));
    }

    public boolean bool(String column) {
        return getInt(column) != 0;
    }

    public UUID uuid(String column) {
        String value = string(column);
        return value == null ? null : UUID.fromString(value);
    }

    public <E extends Enum<E>> E enumValue(String column, Class<E> type, E fallback) {
        String value = string(column);
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
