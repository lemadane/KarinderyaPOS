package ph.karinderya.pos;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ph.karinderya.pos.util.PasswordHasher;

public class PasswordHasherTest {

    @Test
    public void verifiesCorrectPasswordOnly() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("secret123", salt);
        assertTrue(PasswordHasher.verify("secret123", salt, hash));
        assertFalse(PasswordHasher.verify("secret124", salt, hash));
    }

    @Test
    public void saltChangesHash() {
        assertNotEquals(PasswordHasher.hash("same", PasswordHasher.newSalt()),
                PasswordHasher.hash("same", PasswordHasher.newSalt()));
    }
}
