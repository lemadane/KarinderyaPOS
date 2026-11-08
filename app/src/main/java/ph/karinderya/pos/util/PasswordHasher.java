package ph.karinderya.pos.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

  static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  static final int ITERATIONS = 60_000;
  static final int KEY_BITS = 256;
  static final SecureRandom RANDOM = new SecureRandom();

  PasswordHasher() {
  }

  public static String newSalt() {
    byte[] salt = new byte[16];
    RANDOM.nextBytes(salt);
    return Base64.getEncoder()
        .encodeToString(salt);
  }

  public static String hash(
      String password,
      String saltBase64) {
    PBEKeySpec spec =
        new PBEKeySpec(
            password.toCharArray(),
            Base64.getDecoder()
                .decode(saltBase64),
            ITERATIONS, KEY_BITS);
    try {
      byte[] hash =
          SecretKeyFactory.getInstance(
              ALGORITHM).generateSecret(spec)
          .getEncoded();
      return Base64.getEncoder()
          .encodeToString(hash);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(
          "Password hashing unavailable",
          e);
    } finally {
      spec.clearPassword();
    }
  }

  public static boolean verify(
      String password,
      String saltBase64,
      String expectedHash) {
    byte[] actual =
        Base64.getDecoder()
        .decode(hash(password, saltBase64));
    byte[] expected = Base64.getDecoder()
        .decode(expectedHash);
    return MessageDigest.isEqual(
        actual,
        expected);
  }
}
