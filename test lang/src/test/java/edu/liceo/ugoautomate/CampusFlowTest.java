package edu.liceo.ugoautomate;

import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.model.EntryLog;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.model.LocationType;
import edu.liceo.ugoautomate.model.ScanResult;
import edu.liceo.ugoautomate.model.SessionType;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.security.AccessDeniedException;
import edu.liceo.ugoautomate.service.ServiceException;
import edu.liceo.ugoautomate.util.DatabaseInitializer;
import edu.liceo.ugoautomate.util.QrCodeUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests against a real temporary SQLite database.
 */
class CampusFlowTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Manila");

    @TempDir
    Path tempDir;

    private MutableClock clock;
    private AppContext ctx;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-09-28T01:00:00Z"), ZONE); // 09:00 Manila
        ctx = AppContext.create(tempDir.resolve("test.db"), clock);
    }

    @AfterEach
    void tearDown() {
        ctx.close();
    }

    private void loginAdmin() {
        ctx.auth().login(DatabaseInitializer.DEFAULT_ADMIN_USERNAME, DatabaseInitializer.DEFAULT_ADMIN_PASSWORD.toCharArray());
    }

    private void loginStudent(String number) {
        ctx.auth().login(number, DatabaseInitializer.DEFAULT_STUDENT_PASSWORD.toCharArray());
    }

    @Test
    void seededDataIsPresentAndPasswordsAreHashed() {
        loginAdmin();
        assertEquals(4, ctx.accounts().listStudents().size());
        assertEquals(1, ctx.accounts().listGuests().size());
        assertTrue(ctx.accounts().listStudents().get(0).getPasswordHash().startsWith("$2a$"));
        assertFalse(ctx.locations().search("registrar", null).isEmpty());
        assertFalse(ctx.locations().search("", LocationType.BUILDING).isEmpty());
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThrows(ServiceException.class, () -> ctx.auth().login("admin", "wrong-pass1".toCharArray()));
        assertThrows(ServiceException.class, () -> ctx.auth().login("nobody", "wrong-pass1".toCharArray()));
    }

    @Test
    void adminFunctionsRequireAdmin() {
        loginStudent("2023-00001");
        assertThrows(AccessDeniedException.class, () -> ctx.accounts().listStudents());
        assertThrows(AccessDeniedException.class, () -> ctx.attendance().recentRecords());
        ctx.auth().logout();
        assertThrows(AccessDeniedException.class, () -> ctx.locations().listAll());
    }

    @Test
    void studentRegistrationAndLogin() {
        Student s = ctx.auth().registerStudent("2025-12345", "Test Student", "bsit", 1, "t@liceo.edu.ph", null,
                "Passw0rd!".toCharArray(), "Passw0rd!".toCharArray());
        assertEquals("BSIT", s.getCourse());
        assertThrows(ServiceException.class, () -> ctx.auth().registerStudent("2025-12345", "Dup", "BSIT", 1,
                "d@liceo.edu.ph", null, "Passw0rd!".toCharArray(), "Passw0rd!".toCharArray()));
        assertEquals("Test Student", ctx.auth().login("2025-12345", "Passw0rd!".toCharArray()).getFullName());
    }

    @Test
    void attendanceFlowValidatesQrCodes() {
        loginAdmin();
        LocalDateTime now = LocalDateTime.now(clock);
        AttendanceSession session = ctx.attendance().createSession("IT 213 - OOP", SessionType.CLASS, "Computer Laboratory 1",
                now.minusMinutes(5), now.plusHours(1));
        String payload = ctx.attendance().sessionQrPayload(session.getId());

        // Round-trip through an actual QR image, as a scanner would.
        String decoded = QrCodeUtil.decode(QrCodeUtil.generate(payload, 300), true).orElseThrow();
        assertEquals(payload, decoded);
        ctx.auth().logout();

        loginStudent("2023-00001");
        ScanResult first = ctx.attendance().recordAttendance(decoded);
        assertTrue(first.isSuccess(), first.getMessage());
        ScanResult duplicate = ctx.attendance().recordAttendance(decoded);
        assertFalse(duplicate.isSuccess());
        assertFalse(ctx.attendance().recordAttendance(payload.replace("|ATT|", "|ENT|")).isSuccess());
        assertEquals(1, ctx.attendance().myAttendance().size());
        ctx.auth().logout();

        // Regenerating the code invalidates the old one.
        loginAdmin();
        ctx.attendance().regenerateQr(session.getId());
        ctx.auth().logout();
        loginStudent("2023-00002");
        ScanResult stale = ctx.attendance().recordAttendance(payload);
        assertFalse(stale.isSuccess());
        assertTrue(stale.getMessage().contains("replaced"));
        ctx.auth().logout();

        loginAdmin();
        assertEquals(1, ctx.attendance().recordsForSession(session.getId()).size());
        ctx.attendance().addManualRecord(session.getId(), "2023-00002");
        assertEquals(2, ctx.attendance().recordsForSession(session.getId()).size());
    }

    @Test
    void expiredAttendanceCodeIsRejected() {
        loginAdmin();
        LocalDateTime now = LocalDateTime.now(clock);
        AttendanceSession session = ctx.attendance().createSession("Assembly", SessionType.EVENT, "Gymnasium",
                now, now.plusMinutes(30));
        String payload = ctx.attendance().sessionQrPayload(session.getId());
        ctx.auth().logout();

        clock.advance(Duration.ofMinutes(31));
        loginStudent("2023-00001");
        ScanResult result = ctx.attendance().recordAttendance(payload);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("expired"), result.getMessage());
    }

    @Test
    void studentEntryByPassAndCredentials() {
        loginStudent("2023-00001");
        String pass = ctx.entries().myEntryPass().payload();
        ctx.auth().logout();

        loginAdmin();
        assertTrue(ctx.entries().verifyQr(pass).isSuccess());
        assertTrue(ctx.entries().verifyStudentCredentials("2023-00002",
                DatabaseInitializer.DEFAULT_STUDENT_PASSWORD.toCharArray()).isSuccess());
        assertFalse(ctx.entries().verifyStudentCredentials("2023-00002", "bad-pass-1".toCharArray()).isSuccess());

        List<EntryLog> logs = ctx.entries().entryLogs(LocalDate.now(clock), LocalDate.now(clock));
        assertEquals(2, logs.size());

        clock.advance(Duration.ofMinutes(11));
        assertFalse(ctx.entries().verifyQr(pass).isSuccess());
    }

    @Test
    void guestVisitPassIsSingleUse() {
        ctx.auth().login("guest@example.com", DatabaseInitializer.DEFAULT_GUEST_PASSWORD.toCharArray());
        GuestVisit visit = ctx.visits().registerVisit("Enrollment inquiry", "Registrar's Office");
        assertThrows(ServiceException.class, () -> ctx.visits().registerVisit("Again", "Cashier"));
        String pass = ctx.visits().visitPassPayload(visit.getId());
        ctx.auth().logout();

        loginAdmin();
        ScanResult granted = ctx.entries().verifyQr(pass);
        assertTrue(granted.isSuccess(), granted.getMessage());
        ScanResult reused = ctx.entries().verifyQr(pass);
        assertFalse(reused.isSuccess());
        assertEquals(1, ctx.visits().listVisits(LocalDate.now(clock)).size());
        assertEquals(1, ctx.entries().entryLogs(LocalDate.now(clock), LocalDate.now(clock)).size());
    }

    @Test
    void deactivatedStudentLosesAccessImmediately() {
        loginAdmin();
        long id = ctx.accounts().listStudents().stream()
                .filter(s -> s.getStudentNumber().equals("2023-00001")).findFirst().orElseThrow().getId();
        ctx.auth().logout();

        loginStudent("2023-00001");
        String pass = ctx.entries().myEntryPass().payload();
        ctx.auth().logout();

        loginAdmin();
        ctx.accounts().setActive(id, false);
        assertFalse(ctx.entries().verifyQr(pass).isSuccess());
        ctx.auth().logout();

        assertThrows(ServiceException.class, () -> loginStudent("2023-00001"));
    }
}
