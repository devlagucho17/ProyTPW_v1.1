package subastix.security;

import org.mindrot.jbcrypt.BCrypt;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {
    private static final int ITERATIONS = 310_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String hash(char[] password) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        return "pbkdf2-sha256$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(char[] password, String encoded) {
        if (encoded == null) return false;
        if (encoded.startsWith("$2a$") || encoded.startsWith("$2b$") || encoded.startsWith("$2y$")) {
            if (!encoded.matches("^\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}$")) return false;
            int cost = Integer.parseInt(encoded.substring(4, 6));
            if (cost < 4 || cost > 16) return false;
            try {
                String compatibleHash = encoded.startsWith("$2y$")
                        ? "$2a$" + encoded.substring(4) : encoded;
                return BCrypt.checkpw(new String(password), compatibleHash);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 4 || !parts[0].equals("pbkdf2-sha256")) return false;
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < 100_000 || iterations > 1_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(expected, derive(password, salt, iterations));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular el hash de contraseña.", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
