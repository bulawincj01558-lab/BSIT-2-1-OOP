package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.EntryLogDao;
import edu.liceo.ugoautomate.dao.GuestVisitDao;
import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.EntrantType;
import edu.liceo.ugoautomate.model.EntryLog;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.ScanResult;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.model.VerificationMethod;
import edu.liceo.ugoautomate.model.VisitStatus;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.InvalidQrException;
import edu.liceo.ugoautomate.security.QrToken;
import edu.liceo.ugoautomate.security.QrTokenService;
import edu.liceo.ugoautomate.security.QrTokenType;
import edu.liceo.ugoautomate.util.AppConfig;
import edu.liceo.ugoautomate.util.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class EntryService {
    private final UserDao userDao;
    private final GuestVisitDao visitDao;
    private final EntryLogDao entryLogDao;
    private final QrTokenService qrTokens;
    private final AccessControl access;
    private final Clock clock;

    public EntryService(UserDao userDao, GuestVisitDao visitDao, EntryLogDao entryLogDao,
                        QrTokenService qrTokens, AccessControl access, Clock clock) {
        this.userDao = userDao;
        this.visitDao = visitDao;
        this.entryLogDao = entryLogDao;
        this.qrTokens = qrTokens;
        this.access = access;
        this.clock = clock;
    }

    public QrToken issueStudentEntryQr() {
        User student = access.requireRole(Role.STUDENT);
        return qrTokens.issue(QrTokenType.STUDENT_ENTRY, student.getId(),
                clock.instant().plus(AppConfig.STUDENT_ENTRY_QR_VALIDITY));
    }

    public ScanResult verifyEntry(String rawQr, VerificationMethod method) {
        User gateAdmin = access.requireRole(Role.ADMIN);
        QrToken token;
        try {
            token = qrTokens.verify(rawQr);
        } catch (InvalidQrException e) {
            return ScanResult.failure("Entry denied", e.getMessage());
        }
        return switch (token.getType()) {
            case STUDENT_ENTRY -> verifyStudent(token, method, gateAdmin);
            case GUEST_PASS -> verifyGuest(token, method, gateAdmin);
            case ATTENDANCE, STUDENT_ATTENDANCE -> ScanResult.failure("Entry denied",
                    "This is an attendance QR code. Ask the person for their entry QR or guest pass.");
        };
    }

    private ScanResult verifyStudent(QrToken token, VerificationMethod method, User gateAdmin) {
        Optional<User> found = userDao.findById(token.getSubjectId());
        if (found.isEmpty() || !(found.get() instanceof Student student)) {
            return ScanResult.failure("Entry denied", "No registered student matches this QR code.");
        }
        if (!student.isActive()) {
            return ScanResult.failure("Entry denied", student.getFullName() + "'s account is deactivated.");
        }
        EntryLog log = newLog(EntrantType.STUDENT, student.getFullName(), student.getSummary(), method, gateAdmin);
        log.setUserId(student.getId());
        entryLogDao.create(log);
        return ScanResult.success("Entry granted", "Student: " + student.getFullName() + " (" + student.getSummary() + ")");
    }

    private ScanResult verifyGuest(QrToken token, VerificationMethod method, User gateAdmin) {
        Optional<GuestVisit> found = visitDao.findById(token.getSubjectId());
        if (found.isEmpty()) {
            return ScanResult.failure("Entry denied", "No registered visit matches this guest pass.");
        }
        GuestVisit visit = found.get();
        if (visit.getStatus() != VisitStatus.PENDING) {
            return ScanResult.failure("Entry denied", "This visit is already " + visit.getStatus().getLabel().toLowerCase() + ".");
        }
        LocalDate today = LocalDate.now(clock);
        if (!visit.getVisitDate().equals(today)) {
            return ScanResult.failure("Entry denied", "This guest pass is for " + visit.getVisitDate() + ", not today.");
        }
        Optional<User> guest = userDao.findById(visit.getGuestId());
        if (guest.isEmpty() || !guest.get().isActive()) {
            return ScanResult.failure("Entry denied", "The guest's account is missing or deactivated.");
        }

        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        visitDao.updateStatus(visit.getId(), VisitStatus.CHECKED_IN, now);
        String details = "Visiting " + visit.getPersonToVisit() + " - " + visit.getPurpose();
        EntryLog log = newLog(EntrantType.GUEST, visit.getGuestName(), details, method, gateAdmin);
        log.setUserId(visit.getGuestId());
        log.setVisitId(visit.getId());
        entryLogDao.create(log);
        return ScanResult.success("Entry granted", "Guest: " + visit.getGuestName() + " - " + details);
    }

    private EntryLog newLog(EntrantType type, String name, String details, VerificationMethod method, User gateAdmin) {
        EntryLog log = new EntryLog();
        log.setEntrantType(type);
        log.setEntrantName(name);
        log.setDetails(details);
        log.setMethod(method);
        log.setVerifiedBy(gateAdmin.getId());
        log.setLoggedAt(LocalDateTime.now(clock).withNano(0));
        return log;
    }

    public List<EntryLog> listEntryLogs(LocalDate from, LocalDate to) {
        access.requireRole(Role.ADMIN);
        if (from == null || to == null) {
            throw new ValidationException("Both dates are required.");
        }
        if (to.isBefore(from)) {
            throw new ValidationException("The end date must not be before the start date.");
        }
        return entryLogDao.findBetween(from, to);
    }
}
