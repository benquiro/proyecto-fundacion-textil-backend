package cl.textilcircular.api.core.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Helpers for server-generated secrets (refresh tokens, reset codes). These are random values,
 * so a fast hash is enough; user passwords go through the {@code PasswordEncoder} instead.
 */
public final class Hashes {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Hashes() {
    }

    public static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    /** Constant-time comparison, so response timing does not leak how much of a hash matched. */
    public static boolean matches(String expectedHash, String rawValue) {
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.US_ASCII),
                sha256Hex(rawValue).getBytes(StandardCharsets.US_ASCII));
    }

    /** URL-safe random token with {@code bytes} bytes of entropy. */
    public static String randomToken(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    /** Random numeric code of {@code digits} digits, zero padded. */
    public static String randomDigits(int digits) {
        int bound = (int) Math.pow(10, digits);
        return String.format("%0" + digits + "d", RANDOM.nextInt(bound));
    }
}
