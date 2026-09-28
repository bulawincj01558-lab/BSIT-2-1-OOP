package edu.liceo.ugoautomate.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Issues and verifies tamper-proof QR code payloads.
 *
 * <h2>Payload format</h2>
 * <pre>
 *   LUGA1|&lt;TYPE&gt;|&lt;subjectId&gt;|&lt;nonce&gt;|&lt;expiresEpochSeconds&gt;|&lt;signature&gt;
 * </pre>
 * <ul>
 *   <li>{@code TYPE} is the {@link QrTokenType} code (ATT, ENT, VIS);</li>
 *   <li>{@code nonce} is a random URL-safe value that services compare with
 *       server-side state (session binding / single use);</li>
 *   <li>{@code signature} is Base64url(HMAC-SHA256(secret, everything before the last '|')).</li>
 * </ul>
 * Verification rejects anything that is malformed, carries a wrong signature
 * (compared in constant time), or is past its expiry. Business checks (does the
 * session exist, is the nonce current, is the visit unused) are done by the
 * calling service.
 */
public final class QrTokenService {

    public static final String PREFIX = "LUGA1";
    private static final String SEPARATOR = "|";
    private static final int MAX_PAYLOAD_LENGTH = 512;
    private static final Pattern NONCE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;
    private final Clock clock;

    /**
     * @param secret HMAC key (at least 32 bytes)
     * @param clock  time source for expiry checks
     */
    public QrTokenService(byte[] secret, Clock clock) {
        if (secret == null || secret.length < 32) {
            throw new IllegalArgumentException("QR signing secret must be at least 32 bytes.");
        }
        this.key = new SecretKeySpec(secret.clone(), HMAC_ALGORITHM);
        this.clock = clock;
    }

    /** @return a new random, URL-safe nonce (128 bits) */
    public static String newNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Creates a signed payload.
     *
     * @param type      purpose of the code
     * @param subjectId id of the referenced session/student/visit
     * @param nonce     binding value (must match {@code [A-Za-z0-9_-]{8,64}})
     * @param expiresAt expiry instant
     * @return the text to encode into a QR code
     */
    public String issue(QrTokenType type, long subjectId, String nonce, Instant expiresAt) {
        if (!NONCE_PATTERN.matcher(nonce).matches()) {
            throw new IllegalArgumentException("Invalid nonce format.");
        }
        String body = String.join(SEPARATOR, PREFIX, type.getCode(), Long.toString(subjectId), nonce,
                Long.toString(expiresAt.getEpochSecond()));
        return body + SEPARATOR + sign(body);
    }

    /**
     * Parses and authenticates a payload.
     *
     * @param payload scanned or typed QR text
     * @return the verified token
     * @throws InvalidQrException with a user-readable reason if the payload is
     *                            not a Liceo U Go code, was altered, or has expired
     */
    public QrToken verify(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new InvalidQrException("No QR code data was provided.");
        }
        String text = payload.trim();
        if (text.length() > MAX_PAYLOAD_LENGTH) {
            throw new InvalidQrException("This is not a Liceo U Go Automate QR code.");
        }
        // Fixed-size array of the 6 payload fields: O(1) access by index.
        String[] parts = text.split(Pattern.quote(SEPARATOR), -1);
        if (parts.length != 6 || !PREFIX.equals(parts[0])) {
            throw new InvalidQrException("This is not a Liceo U Go Automate QR code.");
        }

        String body = text.substring(0, text.lastIndexOf(SEPARATOR));
        byte[] expected = sign(body).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = parts[5].getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new InvalidQrException("This QR code is invalid or has been tampered with.");
        }

        QrTokenType type = QrTokenType.fromCode(parts[1])
                .orElseThrow(() -> new InvalidQrException("Unknown QR code type."));
        long subjectId;
        long expiresEpoch;
        try {
            subjectId = Long.parseLong(parts[2]);
            expiresEpoch = Long.parseLong(parts[4]);
        } catch (NumberFormatException e) {
            throw new InvalidQrException("This QR code is malformed.");
        }
        if (!NONCE_PATTERN.matcher(parts[3]).matches()) {
            throw new InvalidQrException("This QR code is malformed.");
        }
        Instant expiresAt = Instant.ofEpochSecond(expiresEpoch);
        if (clock.instant().isAfter(expiresAt)) {
            throw new InvalidQrException("This QR code has expired.");
        }
        return new QrToken(type, subjectId, parts[3], expiresAt);
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available in this Java runtime.", e);
        }
    }
}
