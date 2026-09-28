package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.AttendanceRecordDao;
import edu.liceo.ugoautomate.dao.AttendanceSessionDao;
import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.model.ScanResult;
import edu.liceo.ugoautomate.model.SessionType;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.InvalidQrException;
import edu.liceo.ugoautomate.security.QrToken;
import edu.liceo.ugoautomate.security.QrTokenService;
import edu.liceo.ugoautomate.security.QrTokenType;
import edu.liceo.ugoautomate.util.DateTimeUtil;
import edu.liceo.ugoautomate.util.ValidationException;
import edu.liceo.ugoautomate.util.Validators;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * QR Code Attendance (F-3.x) and administrator attendance management (F-5.3).
 */
public class AttendanceService {

    /** Upper bound for the "recent records" view so it always renders quickly. */
    public static final int RECENT_LIMIT = 500;

    private final AttendanceSessionDao sessionDao;
    private final AttendanceRecordDao recordDao;
    private final UserDao userDao;
    private final AccessControl access;
    private final QrTokenService qrTokens;
    private final Clock clock;

    public AttendanceService(AttendanceSessionDao sessionDao, AttendanceRecordDao recordDao, UserDao userDao,
                             AccessControl access, QrTokenService qrTokens, Clock clock) {
        this.sessionDao = sessionDao;
        this.recordDao = recordDao;
        this.userDao = userDao;
        this.access = access;
        this.qrTokens = qrTokens;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ admin: sessions

    public AttendanceSession createSession(String title, SessionType type, String venue,
                                           LocalDateTime start, LocalDateTime end) {
        Administrator admin = access.requireAdmin();
        AttendanceSession s = new AttendanceSession();
        applyFields(s, title, type, venue, start, end);
        s.setQrNonce(QrTokenService.newNonce());
        s.setActive(true);
        s.setCreatedBy(admin.getId());
        s.setCreatedAt(LocalDateTime.now(clock));
        sessionDao.insert(s);
        return s;
    }

    public AttendanceSession updateSession(long sessionId, String title, SessionType type, String venue,
                                           LocalDateTime start, LocalDateTime end) {
        access.requireAdmin();
        AttendanceSession s = requireSession(sessionId);
        applyFields(s, title, type, venue, start, end);
        sessionDao.update(s);
        return s;
    }

    public void setSessionActive(long sessionId, boolean active) {
        access.requireAdmin();
        requireSession(sessionId);
        sessionDao.setActive(sessionId, active);
    }

    /** Issues a new nonce, immediately invalidating every previously generated QR code. */
    public void regenerateQr(long sessionId) {
        access.requireAdmin();
        requireSession(sessionId);
        sessionDao.updateNonce(sessionId, QrTokenService.newNonce());
    }

    public void deleteSession(long sessionId) {
        access.requireAdmin();
        if (!sessionDao.delete(sessionId)) {
            throw new ServiceException("The selected session no longer exists.");
        }
    }

    public List<AttendanceSession> listSessions() {
        access.requireAdmin();
        return sessionDao.findAll();
    }

    /**
     * Generates the signed QR payload for an attendance session (F-3.1). The
     * code is bound to the session's current nonce and expires at session end.
     */
    public String sessionQrPayload(long sessionId) {
        access.requireAdmin();
        AttendanceSession s = requireSession(sessionId);
        if (!s.isActive()) {
            throw new ServiceException("This session is closed. Open it first to display its QR code.");
        }
        if (LocalDateTime.now(clock).isAfter(s.getEndTime())) {
            throw new ServiceException("This session has already ended. Edit its schedule to reuse it.");
        }
        return qrTokens.issue(QrTokenType.ATTENDANCE, s.getId(), s.getQrNonce(),
                s.getEndTime().atZone(clock.getZone()).toInstant());
    }

    // ------------------------------------------------------------------ student: scanning

    /**
     * Records the signed-in student's attendance from a scanned QR payload (F-3.2, F-3.3).
     * <p>
     * Validation order:
     * <ol>
     *   <li>the caller is an active student account;</li>
     *   <li>the payload is well-formed, carries a valid HMAC signature, and has not expired;</li>
     *   <li>it is an attendance code (not an entry or visit pass);</li>
     *   <li>the session exists, the code's nonce matches the session's current nonce
     *       (so replaced codes are rejected), and the session is active;</li>
     *   <li>the current time is inside the check-in window;</li>
     *   <li>the student has not already been recorded (enforced by a unique constraint).</li>
     * </ol>
     *
     * @return a success or failure result that is always shown to the user
     */
    public ScanResult recordAttendance(String payload) {
        Student student = access.requireStudent();
        QrToken token;
        try {
            token = qrTokens.verify(payload);
        } catch (InvalidQrException e) {
            return ScanResult.failure("Attendance not recorded: " + e.getMessage());
        }
        if (token.type() != QrTokenType.ATTENDANCE) {
            return ScanResult.failure("This QR code is not an attendance code.");
        }
        AttendanceSession session = sessionDao.findById(token.subjectId()).orElse(null);
        if (session == null) {
            return ScanResult.failure("The attendance session for this QR code no longer exists.");
        }
        if (!session.getQrNonce().equals(token.nonce())) {
            return ScanResult.failure("This QR code has been replaced. Please scan the code currently displayed.");
        }
        if (!session.isActive()) {
            return ScanResult.failure("Attendance for \"" + session.getTitle() + "\" is closed.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(session.checkInOpensAt())) {
            return ScanResult.failure("Attendance for \"" + session.getTitle() + "\" opens at "
                    + DateTimeUtil.format(session.checkInOpensAt()) + ".");
        }
        if (now.isAfter(session.getEndTime())) {
            return ScanResult.failure("Attendance for \"" + session.getTitle() + "\" has ended.");
        }
        if (!recordDao.insertIfAbsent(session.getId(), student.getId(), now)) {
            return ScanResult.failure("Your attendance for \"" + session.getTitle() + "\" was already recorded.");
        }
        return ScanResult.success("Attendance recorded for \"" + session.getTitle() + "\"\n"
                + student.getFullName() + " (" + student.getStudentNumber() + ") at " + DateTimeUtil.format(now));
    }

    public List<AttendanceRecord> myAttendance() {
        Student student = access.requireStudent();
        return recordDao.findByStudent(student.getId());
    }

    // ------------------------------------------------------------------ admin: records

    public List<AttendanceRecord> recordsForSession(long sessionId) {
        access.requireAdmin();
        return recordDao.findBySession(sessionId);
    }

    public List<AttendanceRecord> recentRecords() {
        access.requireAdmin();
        return recordDao.findRecent(RECENT_LIMIT);
    }

    /** Manually records attendance for a student (e.g. when the student's device is unavailable). */
    public void addManualRecord(long sessionId, String studentNumber) {
        access.requireAdmin();
        AttendanceSession session = requireSession(sessionId);
        Student student = userDao.findStudentByNumber(Validators.requireStudentNumber(studentNumber))
                .orElseThrow(() -> new ServiceException("No student found with ID " + studentNumber.trim() + "."));
        if (!recordDao.insertIfAbsent(session.getId(), student.getId(), LocalDateTime.now(clock))) {
            throw new ServiceException(student.getFullName() + " is already recorded for this session.");
        }
    }

    public void deleteRecord(long recordId) {
        access.requireAdmin();
        if (!recordDao.delete(recordId)) {
            throw new ServiceException("The selected record no longer exists.");
        }
    }

    // ------------------------------------------------------------------ helpers

    private AttendanceSession requireSession(long sessionId) {
        return sessionDao.findById(sessionId)
                .orElseThrow(() -> new ServiceException("The selected session no longer exists."));
    }

    private static void applyFields(AttendanceSession s, String title, SessionType type, String venue,
                                    LocalDateTime start, LocalDateTime end) {
        s.setTitle(Validators.requireText(title, "Title", 120));
        if (type == null) {
            throw new ValidationException("Session type is required.");
        }
        s.setSessionType(type);
        s.setVenue(Validators.optionalText(venue, "Venue", 120));
        if (start == null || end == null) {
            throw new ValidationException("Start and end time are required.");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("End time must be after the start time.");
        }
        s.setStartTime(start.withSecond(0).withNano(0));
        s.setEndTime(end.withSecond(0).withNano(0));
    }
}
