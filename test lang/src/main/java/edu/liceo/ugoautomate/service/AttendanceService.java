package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.AttendanceRecordDao;
import edu.liceo.ugoautomate.dao.AttendanceSessionDao;
import edu.liceo.ugoautomate.dao.ClassOfferingDao;
import edu.liceo.ugoautomate.dao.DataIntegrityException;
import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.model.AttendanceSlot;
import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.SessionType;
import edu.liceo.ugoautomate.model.ScanResult;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.model.VerificationMethod;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.InvalidQrException;
import edu.liceo.ugoautomate.security.QrToken;
import edu.liceo.ugoautomate.security.QrTokenService;
import edu.liceo.ugoautomate.security.QrTokenType;
import edu.liceo.ugoautomate.util.AppConfig;
import edu.liceo.ugoautomate.util.Validation;
import edu.liceo.ugoautomate.util.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AttendanceService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("h:mm a");

    private final AttendanceSessionDao sessionDao;
    private final AttendanceRecordDao recordDao;
    private final UserDao userDao;
    private final ClassOfferingDao classDao;
    private final AcademicTerm term;
    private final QrTokenService qrTokens;
    private final AccessControl access;
    private final Clock clock;

    public AttendanceService(AttendanceSessionDao sessionDao, AttendanceRecordDao recordDao, UserDao userDao,
                             ClassOfferingDao classDao, AcademicTerm term,
                             QrTokenService qrTokens, AccessControl access, Clock clock) {
        this.sessionDao = sessionDao;
        this.recordDao = recordDao;
        this.userDao = userDao;
        this.classDao = classDao;
        this.term = term;
        this.qrTokens = qrTokens;
        this.access = access;
        this.clock = clock;
    }

    public List<AttendanceSession> listSessions() {
        access.requireRole(Role.ADMIN);
        return sessionDao.findAll();
    }

    public List<AttendanceSession> listOpenSessions() {
        access.requireRole(Role.ADMIN, Role.STUDENT);
        LocalDateTime now = now();
        return sessionDao.findAll().stream().filter(s -> s.isOpenAt(now)).toList();
    }

    public AttendanceSession createSession(AttendanceSession session) {
        User admin = access.requireRole(Role.ADMIN);
        validate(session);
        session.setCreatedBy(admin.getId());
        session.setCreatedAt(now());
        sessionDao.create(session);
        return session;
    }

    public AttendanceSession updateSession(AttendanceSession session) {
        access.requireRole(Role.ADMIN);
        findSession(session.getId());
        validate(session);
        sessionDao.update(session);
        return session;
    }

    public void deleteSession(long sessionId) {
        access.requireRole(Role.ADMIN);
        findSession(sessionId);
        sessionDao.delete(sessionId);
    }

    public QrToken issueSessionQr(long sessionId) {
        access.requireRole(Role.ADMIN);
        AttendanceSession session = findSession(sessionId);
        if (now().isAfter(session.getEndsAt())) {
            throw new ServiceException("This session has already ended; edit its schedule to generate a new QR code.");
        }
        return qrTokens.issue(QrTokenType.ATTENDANCE, session.getId(), qrTokens.toInstant(session.getEndsAt()));
    }

    public QrToken issueStudentAttendanceQr() {
        User student = access.requireRole(Role.STUDENT);
        return qrTokens.issue(QrTokenType.STUDENT_ATTENDANCE, student.getId(),
                clock.instant().plus(AppConfig.STUDENT_ATTENDANCE_QR_VALIDITY));
    }

    public ScanResult recordStudentQr(long sessionId, String rawQr, VerificationMethod method) {
        return recordStudentQr(sessionId, rawQr, method, null);
    }

    public ScanResult recordStudentQr(long sessionId, String rawQr, VerificationMethod method, AttendanceSlot slot) {
        access.requireRole(Role.ADMIN);
        QrToken token;
        try {
            token = qrTokens.verify(rawQr);
        } catch (InvalidQrException e) {
            return ScanResult.failure("Invalid QR code", e.getMessage());
        }
        if (token.getType() != QrTokenType.STUDENT_ATTENDANCE) {
            return ScanResult.failure("Wrong QR code", "This is " + token.getType().getDescription()
                    + ". Ask the student to open Scan Attendance and show their attendance QR.");
        }
        Optional<User> found = userDao.findById(token.getSubjectId());
        if (found.isEmpty() || !(found.get() instanceof Student student)) {
            return ScanResult.failure("Student not found", "No registered student matches this QR code.");
        }
        if (!student.isActive()) {
            return ScanResult.failure("Account deactivated", student.getFullName() + "'s account is deactivated.");
        }
        Optional<AttendanceSession> session = sessionDao.findById(sessionId);
        if (session.isEmpty()) {
            return ScanResult.failure("Session not found", "The selected attendance session no longer exists.");
        }
        if (slot != null) {
            return recordTime(session.get(), student, method, slot);
        }
        return record(session.get(), student, method, student.getFullName() + "'s", true);
    }

    public ScanResult recordAttendance(String rawQr, VerificationMethod method) {
        return recordAttendance(rawQr, method, null);
    }

    public ScanResult recordOwnQrForClass(String rawQr, VerificationMethod method, long classId) {
        User student = access.requireRole(Role.STUDENT);
        QrToken token;
        try {
            token = qrTokens.verify(rawQr);
        } catch (InvalidQrException e) {
            return ScanResult.failure("Invalid QR code", e.getMessage());
        }
        if (token.getType() != QrTokenType.STUDENT_ATTENDANCE) {
            return recordAttendance(rawQr, method);
        }
        if (token.getSubjectId() != student.getId()) {
            return ScanResult.failure("Not your QR code", "This attendance QR belongs to another student.");
        }
        Optional<ClassOffering> found = classDao.findByStudent(student.getId(), term.current()).stream()
                .filter(c -> c.getId() == classId).findFirst();
        if (found.isEmpty()) {
            return ScanResult.failure("Not enrolled", "This subject is not in your class program.");
        }
        return record(todaysSession(found.get()), student, method, "Your", false);
    }

    private ScanResult recordTime(AttendanceSession session, Student student, VerificationMethod method,
                                  AttendanceSlot slot) {
        LocalDateTime now = now();
        if (!session.isOpenAt(now)) {
            return ScanResult.failure("Session not open", "\"" + session.getTitle() + "\" accepts attendance only from "
                    + TIME.format(session.getStartsAt()) + " to " + TIME.format(session.getEndsAt()) + ".");
        }
        String name = student.getFullName();
        Optional<LocalDateTime> already = recordDao.findTime(session.getId(), student.getId(), slot);
        if (already.isPresent()) {
            return ScanResult.failure("Already recorded", name + "'s " + slot.getLabel().toLowerCase()
                    + " was already recorded at " + CLOCK.format(already.get()) + ".");
        }
        AttendanceSlot first = slot.getRequires();
        if (first != null && recordDao.findTime(session.getId(), student.getId(), first).isEmpty()) {
            return ScanResult.failure("No " + first.getLabel().toLowerCase() + " yet",
                    name + " has no " + first.getLabel().toLowerCase() + " yet. Record that first.");
        }
        if (!recordDao.exists(session.getId(), student.getId())) {
            AttendanceRecord record = new AttendanceRecord();
            record.setSessionId(session.getId());
            record.setStudentId(student.getId());
            record.setRecordedAt(now);
            record.setMethod(method);
            try {
                recordDao.create(record);
            } catch (DataIntegrityException ignored) {
            }
        }
        try {
            recordDao.createTime(session.getId(), student.getId(), slot, now, method);
        } catch (DataIntegrityException e) {
            return ScanResult.failure("Already recorded", name + "'s " + slot.getLabel().toLowerCase()
                    + " was already recorded.");
        }
        return ScanResult.success(slot.getLabel() + " recorded", name + " - \"" + session.getTitle()
                + "\" at " + CLOCK.format(now) + ".");
    }

    private AttendanceSession todaysSession(ClassOffering c) {
        String title = sessionTitle(c);
        LocalDate today = now().toLocalDate();
        return sessionDao.findAll().stream()
                .filter(s -> s.getTitle().equals(title) && s.getStartsAt().toLocalDate().equals(today))
                .findFirst()
                .orElseGet(() -> {
                    AttendanceSession s = new AttendanceSession();
                    s.setTitle(title);
                    s.setType(SessionType.CLASS);
                    s.setStartsAt(today.atTime(c.getStartTime()));
                    s.setEndsAt(today.atTime(c.getEndTime()));
                    s.setCreatedAt(now());
                    sessionDao.create(s);
                    return s;
                });
    }

    private static String sessionTitle(ClassOffering c) {
        String title = c.getSubjectCode() + " - " + c.getSubjectTitle();
        return title.length() > 120 ? title.substring(0, 117) + "..." : title;
    }

    public ScanResult recordAttendance(String rawQr, VerificationMethod method, Long chosenSessionId) {
        User student = access.requireRole(Role.STUDENT);
        QrToken token;
        try {
            token = qrTokens.verify(rawQr);
        } catch (InvalidQrException e) {
            return ScanResult.failure("Invalid QR code", e.getMessage());
        }
        if (token.getType() == QrTokenType.STUDENT_ATTENDANCE) {
            return recordOwnQr(token, student, method, chosenSessionId);
        }
        if (token.getType() != QrTokenType.ATTENDANCE) {
            return ScanResult.failure("Wrong QR code", "This is " + token.getType().getDescription()
                    + ", not an attendance QR code.");
        }
        Optional<AttendanceSession> found = sessionDao.findById(token.getSubjectId());
        if (found.isEmpty()) {
            return ScanResult.failure("Session not found", "The attendance session for this QR code no longer exists.");
        }
        return record(found.get(), student, method, "Your", true);
    }

    private ScanResult recordOwnQr(QrToken token, User student, VerificationMethod method, Long chosenSessionId) {
        if (token.getSubjectId() != student.getId()) {
            return ScanResult.failure("Not your QR code", "This attendance QR belongs to another student.");
        }
        AttendanceSession session;
        if (chosenSessionId != null) {
            Optional<AttendanceSession> chosen = sessionDao.findById(chosenSessionId);
            if (chosen.isEmpty()) {
                return ScanResult.failure("Session not found", "The selected attendance session no longer exists.");
            }
            session = chosen.get();
        } else {
            LocalDateTime now = now();
            List<AttendanceSession> open = sessionDao.findAll().stream().filter(s -> s.isOpenAt(now)).toList();
            if (open.isEmpty()) {
                return ScanResult.failure("No session open", "There is no attendance session open right now.");
            }
            if (open.size() > 1) {
                return ScanResult.failure("Choose a session", "More than one session is open. Choose one first.");
            }
            session = open.get(0);
        }
        return record(session, student, method, "Your", true);
    }

    private ScanResult record(AttendanceSession session, User student, VerificationMethod method, String whose,
                              boolean onlyWhileOpen) {
        LocalDateTime now = now();
        if (onlyWhileOpen && !session.isOpenAt(now)) {
            return ScanResult.failure("Session not open", "\"" + session.getTitle() + "\" accepts attendance only from "
                    + TIME.format(session.getStartsAt()) + " to " + TIME.format(session.getEndsAt()) + ".");
        }
        if (recordDao.exists(session.getId(), student.getId())) {
            return ScanResult.failure("Already recorded", whose + " attendance for \"" + session.getTitle()
                    + "\" was already recorded.");
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setSessionId(session.getId());
        record.setStudentId(student.getId());
        record.setRecordedAt(now);
        record.setMethod(method);
        try {
            recordDao.create(record);
        } catch (DataIntegrityException e) {
            return ScanResult.failure("Already recorded", whose + " attendance for \"" + session.getTitle()
                    + "\" was already recorded.");
        }
        return ScanResult.success("Attendance recorded", student.getFullName() + " - \"" + session.getTitle()
                + "\" at " + TIME.format(now) + ".");
    }

    public List<AttendanceRecord> myAttendance() {
        User student = access.requireRole(Role.STUDENT);
        return recordDao.findByStudent(student.getId());
    }

    public List<AttendanceRecord> listRecords(Long sessionId) {
        access.requireRole(Role.ADMIN);
        return recordDao.findAll(sessionId);
    }

    public void deleteRecord(long recordId) {
        access.requireRole(Role.ADMIN);
        recordDao.delete(recordId);
    }

    private AttendanceSession findSession(long id) {
        return sessionDao.findById(id).orElseThrow(() -> new ServiceException("Attendance session not found."));
    }

    private static void validate(AttendanceSession s) {
        s.setTitle(Validation.required(s.getTitle(), "Title", 120));
        if (s.getType() == null) {
            throw new ValidationException("Session type is required.");
        }
        if (s.getStartsAt() == null || s.getEndsAt() == null) {
            throw new ValidationException("Start and end times are required.");
        }
        s.setStartsAt(s.getStartsAt().withSecond(0).withNano(0));
        s.setEndsAt(s.getEndsAt().withSecond(0).withNano(0));
        if (!s.getEndsAt().isAfter(s.getStartsAt())) {
            throw new ValidationException("End time must be after the start time.");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }
}
