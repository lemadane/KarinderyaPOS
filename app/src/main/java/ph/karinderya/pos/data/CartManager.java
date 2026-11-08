package ph.karinderya.pos.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import ph.karinderya.pos.data.model.CartLine;
import ph.karinderya.pos.data.model.MenuItem;
import ph.karinderya.pos.util.Json;

/**
 * In-progress order shared by the POS screen and checkout; persisted as JSON so it survives
 * restarts. Main-thread only.
 */
public record CartManager(
    SharedPreferences prefs, Map<UUID, CartLine> entries, List<Runnable> listeners) {

  static final String TAG = "CartManager";
  static final String PREFS = "cart";
  static final String KEY_LINES = "lines";
  static final int MAX_QTY = 999;

  public CartManager(Context context) {
    this(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        new LinkedHashMap<>(),
        new CopyOnWriteArrayList<>());
    load();
  }

  public void addListener(Runnable listener) {
    listeners.add(listener);
  }

  public void removeListener(Runnable listener) {
    listeners.remove(listener);
  }

  public List<CartLine> lines() {
    return List.copyOf(entries.values());
  }

  public boolean isEmpty() {
    return entries.isEmpty();
  }

  public int quantityOf(UUID menuItemId) {
    CartLine line = entries.get(menuItemId);
    return line == null ? 0 : line.quantity();
  }

  public int totalQuantity() {
    return entries.values().stream().mapToInt(CartLine::quantity).sum();
  }

  public long subtotalCents() {
    return entries.values().stream().mapToLong(CartLine::lineTotalCents).sum();
  }

  public void add(MenuItem item) {
    CartLine existing = entries.get(item.id());
    entries.put(
        item.id(),
        existing == null
            ? CartLine.of(item)
            : existing.withQuantity(Math.min(MAX_QTY, existing.quantity() + 1)));
    changed();
  }

  public void increment(UUID menuItemId) {
    CartLine line = entries.get(menuItemId);
    if (line != null) {
      entries.put(menuItemId, line.withQuantity(Math.min(MAX_QTY, line.quantity() + 1)));
      changed();
    }
  }

  public void decrement(UUID menuItemId) {
    CartLine line = entries.get(menuItemId);
    if (line == null) {
      return;
    }
    if (line.quantity() <= 1) {
      entries.remove(menuItemId);
    } else {
      entries.put(menuItemId, line.withQuantity(line.quantity() - 1));
    }
    changed();
  }

  public void remove(UUID menuItemId) {
    if (entries.remove(menuItemId) != null) {
      changed();
    }
  }

  public void clear() {
    entries.clear();
    changed();
  }

  void changed() {
    save();
    for (Runnable listener : listeners) {
      listener.run();
    }
  }

  void save() {
    try {
      prefs
          .edit()
          .putString(KEY_LINES, Json.mapper().writeValueAsString(new ArrayList<>(entries.values())))
          .apply();
    } catch (Exception e) {
      Log.w(TAG, "Unable to persist cart", e);
    }
  }

  void load() {
    String json = prefs.getString(KEY_LINES, null);
    if (json == null) {
      return;
    }
    try {
      List<CartLine> saved =
          Json.mapper().readValue(json, new TypeReference<List<CartLine>>() {});
      for (CartLine line : saved) {
        if (line.menuItemId() != null && line.quantity() > 0) {
          entries.put(line.menuItemId(), line);
        }
      }
    } catch (Exception e) {
      Log.w(TAG, "Discarding unreadable cart", e);
      prefs.edit().remove(KEY_LINES).apply();
    }
  }
}
