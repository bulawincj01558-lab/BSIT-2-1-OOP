package edu.liceo.ugoautomate.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrTokenServiceTest {
    private static final byte[] SECRET = "0123456789abcdef0123456789abcdef".getBytes();

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-29T08:00:00Z"));
    private final QrTokenService service = new QrTokenService(SECRET, clock);

    @Test
    void issuedTokenVerifiesAndKeepsBinding() throws InvalidQrException {
        QrToken issued = service.issue(QrTokenType.ATTENDANCE, 42, clock.instant().plus(Duration.ofHours(1)));

        QrToken verified = service.verify(issued.getEncoded());

        assertEquals(QrTokenType.ATTENDANCE, verified.getType());
        assertEquals(42, verified.getSubjectId());
        assertTrue(issued.getEncoded().startsWith("UGO1.ATTENDANCE.42."));
    }

    @Test
    void tokensForSameSubjectDifferByNonce() {
        Instant exp = clock.instant().plus(Duration.ofHours(1));
        assertNotEquals(service.issue(QrTokenType.GUEST_PASS, 1, exp).getEncoded(),
                service.issue(QrTokenType.GUEST_PASS, 1, exp).getEncoded());
    }

    @Test
    void tamperedSubjectIsRejected() {
        String token = service.issue(QrTokenType.ATTENDANCE, 42, clock.instant().plus(Duration.ofHours(1))).getEncoded();
        String tampered = token.replace("UGO1.ATTENDANCE.42.", "UGO1.ATTENDANCE.43.");

        InvalidQrException e = assertThrows(InvalidQrException.class, () -> service.verify(tampered));
        assertTrue(e.getMessage().contains("signature"));
    }

    @Test
    void tokenFromAnotherInstallationIsRejected() {
        QrTokenService other = new QrTokenService("another-installation-secret-32b!".getBytes(), clock);
        String foreign = other.issue(QrTokenType.STUDENT_ENTRY, 2, clock.instant().plus(Duration.ofMinutes(5))).getEncoded();

        assertThrows(InvalidQrException.class, () -> service.verify(foreign));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = service.issue(QrTokenType.STUDENT_ENTRY, 2, clock.instant().plus(Duration.ofMinutes(5))).getEncoded();
        clock.advance(Duration.ofMinutes(6));

        InvalidQrException e = assertThrows(InvalidQrException.class, () -> service.verify(token));
        assertTrue(e.getMessage().contains("expired"));
    }

    @Test
    void garbageIsRejected() {
        assertThrows(InvalidQrException.class, () -> service.verify(""));
        assertThrows(InvalidQrException.class, () -> service.verify("https://example.com"));
        assertThrows(InvalidQrException.class, () -> service.verify("UGO1.ATTENDANCE.x.1.2.n.sig"));
    }
}
