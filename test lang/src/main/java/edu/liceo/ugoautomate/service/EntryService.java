package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.EntryLogDao;
import edu.liceo.ugoautomate.dao.GuestVisitDao;
import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.EntrantType;
import edu.liceo.ugoautomate.model.EntryLog;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.model.ScanResult;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.model.VerificationMethod;
import edu.liceo.ugoautomate.model.VisitStatus;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.InvalidQrException;
import edu.liceo.ugoautomate.security.PasswordHasher;
import edu.liceo.ugoautomate.security.QrToken;
import edu.liceo.ugoautomate.security.QrTokenService;
import edu.liceo.ugoautomate.security.QrTokenType;
import edu.liceo.ugoautomate.util.DateTimeUtil;
import edu.liceo.ugoautomate.util.ValidationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Campus Entry and Access Verification (F-4.x) and entry logs (F-5.4).
 * <p>
 * Students present either a short-lived signed entry pass QR (shown in their
 * dashboard) or their account credentials. Guests present the visit pass of a
 * visit registered for today. Gate verification is performed by an
 * administrator account, and only verified entries are written to the log.
 */
public class EntryService {

    /** How long a student's on-screen entry pass remains valid. */
    public static final Duration ENTRY_PASS_VALIDITY = Duration.ofMinutes(10);

    private final UserDao userDao;
    private final GuestVisitDao visitDao;
    private final EntryLogDao entryLogDao;
    private final AccessControl access;
    private final QrTokenService qrTokens;
    private final Clock clock;

    public EntryService(UserDao userDao, GuestVisitDao visitDao, EntryLogDao entryLogDao, AccessControl access,
                        QrTokenService qrTokens, Clock clock) {
        this.userDao = userDao;
        this.visitDao = visitDao;
        this.entryLogDao = entryLogDao;
        this.access = access;
        this.qrTokens = qrTokens;
        this.clock = clock;
    }

    /** Pairs an entry pass payload with its expiry time for display. */
    public record EntryPass(String payload, Instant expiresAt) {
    }

    /** Issues a fresh entry pass QR for the signed-in student (F-4.1). */
    public EntryPass myEntryPass() {
        Student student = access.requireStudent();
        Instant expires = clock.instant().plus(ENTRY_PASS_VALIDITY);
        String payload = qrTokens.issue(QrTokenType.ENTRY_PASS, student.getId(), QrTokenService.newNonce(), expires);
        return new EntryPass(payload, expires);
    }

    /**
     * Verifies a scanned entry QR (student entry pass or guest visit pass) and,
     * if valid, records the entry (F-4.3, F-4.4).
     *
     * @return the verification outcome, always shown to the gate operator
     */
    public ScanResult verifyQr(String payload) {
        Administrator gate = access.requireAdmin();
        QrToken token;
        try {
            token = qrTokens.verify(payload);
        } catch (InvalidQrException e) {
            return ScanResult.failure("ENTRY DENIED: " + e.getMessage());
        }
        return switch (token.type()) {
            case ENTRY_PASS -> admitStudent(token, gate);
            case VISIT_PASS -> admitGuest(token, gate);
            case ATTENDANCE -> ScanResult.failure("ENTRY DENIED: This is an attendance QR code, not an entry pass.");
        };
    }

    /**
     * Verifies a student by student ID and password at the gate (F-4.1 account
     * presentation) and records the entry when valid.
     */
    public ScanResult verifyStudentCredentials(String studentNumber, char[] password) {
        try {
            Administrator gate = access.requireAdmin();
            if (studentNumber == null || studentNumber.isBlank() || password == null || password.length == 0) {
                throw new ValidationException("Enter the student ID and password.");
            }
            Optional<Student> found = userDao.findStudentByNumber(studentNumber.trim());
            if (found.isEmpty() || !PasswordHasher.verify(password, found.get().getPasswordHash())) {
                return ScanResult.failure("ENTRY DENIED: Invalid student ID or password.");
            }
            Student student = found.get();
            if (!student.isActive()) {
                return ScanResult.failure("ENTRY DENIED: The account of " + student.getFullName() + " is deactivated.");
            }
            logStudentEntry(student, VerificationMethod.CREDENTIALS, gate);
            return grantedStudent(student);
        } finally {
            AuthService.wipe(password);
        }
    }

    public List<EntryLog> entryLogs(LocalDate from, LocalDate to) {
        access.requireAdmin();
        if (from == null || to == null) {
            throw new ValidationException("Select a date range.");
        }
        if (to.isBefore(from)) {
            throw new ValidationException("The end date must not be before the start date.");
        }
        return entryLogDao.findBetween(DateTimeUtil.startOfDay(from), DateTimeUtil.endOfDay(to));
    }

    // ------------------------------------------------------------------ internals

    private ScanResult admitStudent(QrToken token, Administrator gate) {
        User user = userDao.findById(token.subjectId()).orElse(null);
        if (!(user instanceof Student student)) {
            return ScanResult.failure("ENTRY DENIED: This entry pass does not belong to a registered student.");
        }
        if (!student.isActive()) {
            return ScanResult.failure("ENTRY DENIED: The account of " + student.getFullName() + " is deactivated.");
        }
        logStudentEntry(student, VerificationMethod.QR_CODE, gate);
        return grantedStudent(student);
    }

    private ScanResult admitGuest(QrToken token, Administrator gate) {
        GuestVisit visit = visitDao.findById(token.subjectId()).orElse(null);
        if (visit == null || !visit.getPassNonce().equals(token.nonce())) {
            return ScanResult.failure("ENTRY DENIED: This visit pass is not recognized.");
        }
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ScanResult.failure("ENTRY DENIED: This visit was cancelled.");
        }
        if (visit.getStatus() == VisitStatus.CHECKED_IN) {
            return ScanResult.failure("ENTRY DENIED: This visit pass was already used at "
                    + DateTimeUtil.format(visit.getCheckedInAt()) + ".");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!visit.getVisitDate().equals(now.toLocalDate())) {
            return ScanResult.failure("ENTRY DENIED: This visit is registered for "
                    + DateTimeUtil.format(visit.getVisitDate()) + ", not today.");
        }
        User guest = userDao.findById(visit.getGuestUserId()).orElse(null);
        if (guest == null || !guest.isActive()) {
            return ScanResult.failure("ENTRY DENIED: The guest account is not active.");
        }
        if (!visitDao.markCheckedIn(visit.getId(), now)) {
            return ScanResult.failure("ENTRY DENIED: This visit pass was already used.");
        }

        EntryLog log = new EntryLog();
        log.setEntrantType(EntrantType.GUEST);
        log.setUserId(guest.getId());
        log.setVisitId(visit.getId());
        log.setEntrantName(guest.getFullName());
        log.setIdentifier(guest.getContactNumber() == null ? guest.getUsername() : guest.getContactNumber());
        log.setDetails("Purpose: " + visit.getPurpose() + " | Visiting: " + visit.getPersonToVisit());
        log.setMethod(VerificationMethod.QR_CODE);
        log.setVerifiedBy(gate.getId());
        log.setEntryTime(now);
        entryLogDao.insert(log);

        return ScanResult.success("ENTRY GRANTED - Guest\n" + guest.getFullName()
                + "\nVisiting: " + visit.getPersonToVisit() + "\nPurpose: " + visit.getPurpose());
    }

    private void logStudentEntry(Student student, VerificationMethod method, Administrator gate) {
        EntryLog log = new EntryLog();
        log.setEntrantType(EntrantType.STUDENT);
        log.setUserId(student.getId());
        log.setEntrantName(student.getFullName());
        log.setIdentifier(student.getStudentNumber());
        log.setDetails(student.getCourseAndYear());
        log.setMethod(method);
        log.setVerifiedBy(gate.getId());
        log.setEntryTime(LocalDateTime.now(clock));
        entryLogDao.insert(log);
    }

    private static ScanResult grantedStudent(Student student) {
        return ScanResult.success("ENTRY GRANTED - Student\n" + student.getFullName()
                + "\n" + student.getStudentNumber() + " | " + student.getCourseAndYear());
    }
}
