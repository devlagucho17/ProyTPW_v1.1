package subastix.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.mindrot.jbcrypt.BCrypt;
import org.junit.jupiter.api.Test;

class PasswordUtilTest {
    @Test
    void hashesPasswordsWithUniqueSaltsAndVerifiesOnlyTheMatchingPassword() {
        char[] password = "correct horse battery".toCharArray();

        String firstHash = PasswordUtil.hash(password);
        String secondHash = PasswordUtil.hash(password);

        assertNotEquals(firstHash, secondHash);
        assertTrue(PasswordUtil.verify(password, firstHash));
        assertFalse(PasswordUtil.verify("incorrect password".toCharArray(), firstHash));
    }

    @Test
    void rejectsUnsupportedOrMalformedHashes() {
        assertFalse(PasswordUtil.verify("password".toCharArray(), "not-a-password-hash"));
        assertFalse(PasswordUtil.verify("password".toCharArray(),
                "pbkdf2-sha256$1$not-base64$not-base64"));
        assertFalse(PasswordUtil.verify("password".toCharArray(), "$2a$10$wT8m9xVqA3y...hash"));
    }

    @Test
    void verifiesExistingBcryptPasswordHashes() {
        String hash = BCrypt.hashpw("123456", BCrypt.gensalt(10));

        assertTrue(PasswordUtil.verify("123456".toCharArray(), hash));
        assertFalse(PasswordUtil.verify("654321".toCharArray(), hash));
    }
}
