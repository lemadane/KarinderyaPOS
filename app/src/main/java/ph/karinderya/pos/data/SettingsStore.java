package ph.karinderya.pos.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Small key/value settings backed by SharedPreferences. */
public record SettingsStore(SharedPreferences prefs) {

  public static final String DEFAULT_STORE_NAME = "KarinderyaPOS";
  static final String PREFS = "settings";
  static final String KEY_STORE_NAME = "store_name";

  public SettingsStore(Context context) {
    this(context.getSharedPreferences(
        PREFS,
        Context.MODE_PRIVATE));
  }

  public String storeName() {
    return prefs.getString(
        KEY_STORE_NAME,
        DEFAULT_STORE_NAME);
  }

  public void setStoreName(String name) {
    String clean = name == null || name.isBlank()
        ? DEFAULT_STORE_NAME
        : name.trim();
    prefs.edit()
        .putString(
            KEY_STORE_NAME,
            clean)
        .apply();
  }
}
