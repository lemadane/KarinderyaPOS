package ph.karinderya.pos.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import ph.karinderya.pos.data.model.User;
import ph.karinderya.pos.data.repo.UserRepository;

/** Remembers the logged-in crew member across app restarts. */
public record SessionManager(
    SharedPreferences prefs, UserRepository users, AtomicReference<User> current) {

  static final String PREFS = "session";
  static final String KEY_USER_ID = "user_id";

  public SessionManager(Context context, UserRepository users) {
    this(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        users,
        new AtomicReference<>());
  }

  /** Restores the logged-in user; touches the database, so call off the main thread. */
  public Optional<User> restore() {
    User cached = current.get();
    if (cached != null) {
      return Optional.of(cached);
    }
    String id = prefs.getString(KEY_USER_ID, null);
    if (id == null) {
      return Optional.empty();
    }
    try {
      Optional<User> user = users.findById(UUID.fromString(id));
      user.ifPresentOrElse(current::set, this::logout);
      return user;
    } catch (IllegalArgumentException e) {
      logout();
      return Optional.empty();
    }
  }

  public void login(User user) {
    current.set(user);
    prefs.edit().putString(KEY_USER_ID, user.id().toString()).apply();
  }

  public void logout() {
    current.set(null);
    prefs.edit().remove(KEY_USER_ID).apply();
  }

  /** The logged-in user; only valid after {@link #restore()} or {@link #login(User)}. */
  public User requireUser() {
    User user = current.get();
    if (user == null) {
      throw new IllegalStateException("No user is logged in");
    }
    return user;
  }

  public boolean isLoggedIn() {
    return current.get() != null;
  }
}
