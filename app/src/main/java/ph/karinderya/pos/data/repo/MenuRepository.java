package ph.karinderya.pos.data.repo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import ph.karinderya.pos.data.db.CursorReader;
import ph.karinderya.pos.data.db.DbHelper;
import ph.karinderya.pos.data.db.Schema;
import ph.karinderya.pos.data.model.MenuItem;

public record MenuRepository(DbHelper db) {

  /** Tab order on the POS screen; custom categories follow alphabetically. */
  public static final List<String> DEFAULT_CATEGORIES =
      List.of(
          "Kanin",
          "Ulam",
          "Gulay",
          "Drinks",
          "Dessert",
          "Mirienda");

  /** SQL ORDER BY term that sorts {@link #DEFAULT_CATEGORIES} first, in their listed order. */
  static final String CATEGORY_ORDER = categoryOrderSql();

  static String categoryOrderSql() {
    final var sql = new StringBuilder("CASE category");
    for (int i = 0; i < DEFAULT_CATEGORIES.size(); i++) {
      sql.append(" WHEN '").append(DEFAULT_CATEGORIES.get(i)).append("' THEN ").append(i);
    }
    return sql.append(" ELSE ").append(DEFAULT_CATEGORIES.size())
        .append(" END, category COLLATE NOCASE").toString();
  }

  // The constructor is no longer needed for a record, as the field is automatically initialized.

  /**
   * @param query    case-insensitive name filter, may be empty
   * @param category exact category, or {@code null} for all
   */
  public List<MenuItem> list(
      final String query,
      final String category,
      final boolean availableOnly) {
    final var where = new StringBuilder("1 = 1");
    final List<String> args = new ArrayList<>();
    if (query != null && !query.isBlank()) {
      where.append(" AND name LIKE ?");
      args.add("%" + query.trim() + "%");
    }
    if (category != null) {
      where.append(" AND category = ?");
      args.add(category);
    }
    if (availableOnly) {
      where.append(" AND available = 1");
    }
    List<MenuItem> result = new ArrayList<>();
    try (final var cursor = db.getReadableDatabase()
            .query(
                Schema.MENU_ITEMS,
                null,
                where.toString(),
                args.toArray(new String[0]),
                null,
                null,
                CATEGORY_ORDER + ", name COLLATE NOCASE")) {
      final var cursorReader = new CursorReader(cursor);
      while (cursor.moveToNext()) {
        result.add(map(cursorReader));
      }
    }
    return result;
  }

  public Optional<MenuItem> findById(final UUID id) {
    try (
        final var cursor = db.getReadableDatabase()
            .query(
                Schema.MENU_ITEMS,
                null,
                "id = ?",
                new String[] {
                  id.toString()},
                null,
                null,
                null)) {
      return cursor.moveToFirst()
          ? Optional.of(map(new CursorReader(cursor)))
          : Optional.empty();
    }
  }

  /** Default categories first, followed by any custom ones the crew created. */
  public List<String> categories() {
    final Set<String> result =
        new LinkedHashSet<>(DEFAULT_CATEGORIES);
    try (final var cursor = db.getReadableDatabase()
            .rawQuery(
                """
                SELECT DISTINCT category
                FROM menu_items
                ORDER BY category
                COLLATE NOCASE
            """, 
                null)) {
      while (cursor.moveToNext()) {
        result.add(
            cursor.getString(0));
      }
    }
    return new ArrayList<>(result);
  }

  /** Inserts when {@code item.id()} is null, otherwise updates. Returns the persisted item. */
  public MenuItem save(final MenuItem item) {
    validate(item);
    final var now = Instant.now();
    final var database = db.getWritableDatabase();
    if (item.id() == null) {
      MenuItem created =
          new MenuItem(
              UUID.randomUUID(),
              item.name()
                  .trim(),
              trimToNull(item.description()),
              item.category()
                  .trim(),
              item.priceCents(),
              item.photoPath(),
              item.available(),
              item.mealOfDay(),
              now,
              now);
      database.insertOrThrow(
          Schema.MENU_ITEMS,
          null,
          toValues(created));
      return created;
    }
    final var updated = new MenuItem(
        item.id(),
        item.name()
            .trim(),
        trimToNull(item.description()),
        item.category()
            .trim(), item
            .priceCents(),
        item
            .photoPath(),
        item
            .available(),
        item
            .mealOfDay(),
        item.createdAt(),
        now);
    database.update(
        Schema.MENU_ITEMS,
        toValues(updated),
        "id = ?",
        new String[] {
          item.id()
              .toString()
        });
    return updated;
  }

  /** Insert-or-replace keyed on UUID; used by backup import. */
  public void upsert(MenuItem item) {
    db.getWritableDatabase()
        .insertWithOnConflict(Schema.MENU_ITEMS, null, toValues(item),
        SQLiteDatabase.CONFLICT_REPLACE);
  }

  public void setAvailable(
      final UUID id,
      final boolean available) {
    final var values = new ContentValues();
    values.put(
        "available",
        available
            ? 1
            : 0);
    values.put(
        "updated_at",
        Instant.now()
            .toEpochMilli());
    db.getWritableDatabase()
        .update(
            Schema.MENU_ITEMS,
            values,
            "id = ?",
            new String[] {
              id.toString()}
    );
  }

  /** Order history keeps name/price snapshots, so deleting a menu item never breaks old orders. */
  public void delete(final UUID id) {
    db.getWritableDatabase()
        .delete(
            Schema.MENU_ITEMS,
            "id = ?",
            new String[] {
              id.toString()
            });
  }

  static void validate(final MenuItem item) {
    if (item.name() == null
        || item.name()
            .isBlank()) {
      throw new IllegalArgumentException("Name is required");
    }
    if (item.category() == null
        || item.category()
            .isBlank()) {
      throw new IllegalArgumentException(
          "Category is required");
    }
    if (item.priceCents() < 0) {
      throw new IllegalArgumentException(
          "Price cannot be negative");
    }
  }

  static String trimToNull(final String value) {
    return value == null || value.isBlank()
        ? null
        : value.trim();
  }

  static ContentValues toValues(final MenuItem item) {
    final var values = new ContentValues();
    values.put(
        "id",
        item.id()
            .toString());
    values.put(
        "name",
        item.name());
    values.put(
        "description",
        item.description());
    values.put(
        "category",
        item.category());
    values.put(
        "price_cents",
        item.priceCents());
    values.put(
        "photo_path",
        item.photoPath());
    values.put(
        "available",
        item.available()
            ? 1
            : 0);
    values.put(
        "meal_of_day",
        item.mealOfDay()
            ? 1
            : 0);
    values.put(
        "created_at",
        item.createdAt()
            .toEpochMilli());
    values.put(
        "updated_at",
        item.updatedAt()
            .toEpochMilli());
    return values;
  }

  static MenuItem map(final CursorReader cursorReader) {
    return new MenuItem(
        cursorReader.uuid("id"),
        cursorReader.string("name"),
        cursorReader.string("description"),
        cursorReader.string("category"),
        cursorReader.getLong("price_cents"),
        cursorReader.string("photo_path"),
        cursorReader.bool("available"),
        cursorReader.bool("meal_of_day"),
        cursorReader.instant("created_at"),
        cursorReader.instant("updated_at"));
  }
}
