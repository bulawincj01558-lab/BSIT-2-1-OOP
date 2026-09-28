package edu.liceo.ugoautomate.security;

import edu.liceo.ugoautomate.MutableClock;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QrTokenServiceTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-28T01:00:00Z"), ZoneId.of("Asia/Manila"));
    private final byte[] secret = filled(7);
    private final QrTokenService service = new QrTokenService(secret, clock);

    @Test
    void issuedTokenVerifies() {
        String nonce = QrTokenService.newNonce();
        String payload = service.issue(QrTokenType.ATTENDANCE, 42, nonce, clock.instant().plusSeconds(600));
        QrToken token = service.verify(payload);
        assertEquals(QrTokenType.ATTENDANCE, token.type());
        assertEquals(42, token.subjectId());
        assertEquals(nonce, token.nonce());
    }

    @Test
    void tamperedSubjectIsRejected() {
        String payload = service.issue(QrTokenType.ENTRY_PASS, 5, QrTokenService.newNonce(), clock.instant().plusSeconds(600));
        String tampered = payload.replace("|ENT|5|", "|ENT|6|");
        assertThrows(InvalidQrException.class, () -> service.verify(tampered));
    }

    @Test
    void codeFromAnotherInstallationIsRejected() {
        QrTokenService other = new QrTokenService(filled(9), clock);
        String payload = other.issue(QrTokenType.VISIT_PASS, 1, QrTokenService.newNonce(), clock.instant().plusSeconds(600));
        assertThrows(InvalidQrException.class, () -> service.verify(payload));
    }

    @Test
    void expiredCodeIsRejected() {
        String payload = service.issue(QrTokenType.ATTENDANCE, 1, QrTokenService.newNonce(), clock.instant().plusSeconds(60));
        clock.advance(Duration.ofSeconds(61));
        InvalidQrException e = assertThrows(InvalidQrException.class, () -> service.verify(payload));
        assertEquals("This QR code has expired.", e.getMessage());
    }

    @Test
    void garbageIsRejected() {
        assertThrows(InvalidQrException.class, () -> service.verify("https://example.com"));
        assertThrows(InvalidQrException.class, () -> service.verify(""));
        assertThrows(InvalidQrException.class, () -> service.verify("LUGA1|ATT|1|x|y|z"));
    }

    private static byte[] filled(int value) {
        byte[] b = new byte[32];
        Arrays.fill(b, (byte) value);
        return b;
    }
}
