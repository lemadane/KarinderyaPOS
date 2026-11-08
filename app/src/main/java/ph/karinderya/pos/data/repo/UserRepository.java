package ph.karinderya.pos.data.repo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import ph.karinderya.pos.data.db.CursorReader;
import ph.karinderya.pos.data.db.DbHelper;
import ph.karinderya.pos.data.db.Schema;
import ph.karinderya.pos.data.model.User;
import ph.karinderya.pos.util.PasswordHasher;

public record UserRepository(DbHelper db) {

    public static final int MIN_PASSWORD_LENGTH = 6;


    public User register(String username, String fullName, String password) {
        String cleanUsername = username.trim();
        String cleanName = fullName.trim();
        if (cleanUsername.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        User user = new User(UUID.randomUUID(), cleanUsername, cleanName, Instant.now());
        String salt = PasswordHasher.newSalt();
        ContentValues values = new ContentValues();
        values.put("id", user.id().toString());
        values.put("username", user.username());
        values.put("full_name", user.fullName());
        values.put("password_hash", PasswordHasher.hash(password, salt));
        values.put("password_salt", salt);
        values.put("created_at", user.createdAt().toEpochMilli());
        try {
            db.getWritableDatabase().insertOrThrow(Schema.USERS, null, values);
        } catch (SQLiteConstraintException e) {
            throw new IllegalArgumentException("Username \"" + cleanUsername + "\" is already taken");
        }
        return user;
    }

    public Optional<User> login(String username, String password) {
        try (Cursor c = db.getReadableDatabase().query(Schema.USERS, null, "username = ?",
                new String[]{username.trim()}, null, null, null)) {
            if (!c.moveToFirst()) {
                return Optional.empty();
            }
            CursorReader r = new CursorReader(c);
            if (!PasswordHasher.verify(password, r.string("password_salt"), r.string("password_hash"))) {
                return Optional.empty();
            }
            return Optional.of(map(r));
        }
    }

    public Optional<User> findById(UUID id) {
        try (Cursor c = db.getReadableDatabase().query(Schema.USERS, null, "id = ?",
                new String[]{id.toString()}, null, null, null)) {
            return c.moveToFirst() ? Optional.of(map(new CursorReader(c))) : Optional.empty();
        }
    }

    public boolean changePassword(UUID userId, String currentPassword, String newPassword) {
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        SQLiteDatabase database = db.getWritableDatabase();
        try (Cursor c = database.query(Schema.USERS, new String[]{"password_hash", "password_salt"}, "id = ?",
                new String[]{userId.toString()}, null, null, null)) {
            if (!c.moveToFirst()) {
                return false;
            }
            CursorReader r = new CursorReader(c);
            if (!PasswordHasher.verify(currentPassword, r.string("password_salt"), r.string("password_hash"))) {
                return false;
            }
        }
        String salt = PasswordHasher.newSalt();
        ContentValues values = new ContentValues();
        values.put("password_hash", PasswordHasher.hash(newPassword, salt));
        values.put("password_salt", salt);
        return database.update(Schema.USERS, values, "id = ?", new String[]{userId.toString()}) == 1;
    }

     static User map(CursorReader r) {
        return new User(r.uuid("id"), r.string("username"), r.string("full_name"), r.instant("created_at"));
    }
}
