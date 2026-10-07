package edu.liceo.ugoautomate.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;

public class QrTokenService {
    private static final String PREFIX = "UGO1";
    private static final String HMAC = "HmacSHA256";
    private static final int MAX_TOKEN_LENGTH = 512;
    private static final Duration ALLOWED_CLOCK_SKEW = Duration.ofMinutes(1);

    private final SecretKeySpec key;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public QrTokenService(byte[] secret, Clock clock) {
        if (secret == null || secret.length < 16) {
            throw new IllegalArgumentException("QR signing secret must be at least 16 bytes");
        }
        this.key = new SecretKeySpec(secret.clone(), HMAC);
        this.clock = clock;
    }

    public QrToken issue(QrTokenType type, long subjectId, Instant expiresAt) {
        Instant issuedAt = clock.instant();
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Expiry must be in the future");
        }
        byte[] nonceBytes = new byte[9];
        random.nextBytes(nonceBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
        String payload = String.join(".", PREFIX, type.name(), Long.toString(subjectId),
                Long.toString(issuedAt.getEpochSecond()), Long.toString(expiresAt.getEpochSecond()), nonce);
        String encoded = payload + "." + sign(payload);
        return new QrToken(type, subjectId, Instant.ofEpochSecond(issuedAt.getEpochSecond()),
                Instant.ofEpochSecond(expiresAt.getEpochSecond()), encoded);
    }

    public QrToken verify(String raw) throws InvalidQrException {
        if (raw == null || raw.isBlank()) {
            throw new InvalidQrException("No QR code content was provided.");
        }
        String text = raw.trim();
        if (text.length() > MAX_TOKEN_LENGTH) {
            throw new InvalidQrException("This QR code is not a Liceo U Go Automate code.");
        }
        String[] parts = text.split("\\.");
        if (parts.length != 7 || !PREFIX.equals(parts[0])) {
            throw new InvalidQrException("This QR code is not a Liceo U Go Automate code.");
        }

        String payload = text.substring(0, text.lastIndexOf('.'));
        byte[] expected = sign(payload).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = parts[6].getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new InvalidQrException("QR code signature is invalid. The code was altered or not issued by this system.");
        }

        QrTokenType type;
        long subjectId;
        Instant issuedAt;
        Instant expiresAt;
        try {
            type = QrTokenType.valueOf(parts[1]);
            subjectId = Long.parseLong(parts[2]);
            issuedAt = Instant.ofEpochSecond(Long.parseLong(parts[3]));
            expiresAt = Instant.ofEpochSecond(Long.parseLong(parts[4]));
        } catch (IllegalArgumentException e) {
            throw new InvalidQrException("This QR code is damaged or incomplete.");
        }

        Instant now = clock.instant();
        if (now.isAfter(expiresAt)) {
            throw new InvalidQrException("This QR code has expired. Please generate a new one.");
        }
        if (issuedAt.isAfter(now.plus(ALLOWED_CLOCK_SKEW))) {
            throw new InvalidQrException("This QR code is not valid yet. Check the device clock.");
        }
        return new QrToken(type, subjectId, issuedAt, expiresAt, text);
    }

    public Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime.atZone(clock.getZone()).toInstant();
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(key);
            byte[] signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }
}
