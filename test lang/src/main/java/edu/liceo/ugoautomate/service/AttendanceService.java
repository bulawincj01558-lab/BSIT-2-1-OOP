package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.AttendanceRecordDao;
import edu.liceo.ugoautomate.dao.AttendanceSessionDao;
import edu.liceo.ugoautomate.dao.DataIntegrityException;
import edu.liceo.ugoautomate.model.AttendanceRecord;
import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.ScanResult;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.model.VerificationMethod;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.InvalidQrException;
import edu.liceo.ugoautomate.security.QrToken;
import edu.liceo.ugoautomate.security.QrTokenService;
import edu.liceo.ugoautomate.security.QrTokenType;
import edu.liceo.ugoautomate.util.Validation;
import edu.liceo.ugoautomate.util.ValidationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class AttendanceService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    private final AttendanceSessionDao sessionDao;
    private final AttendanceRecordDao recordDao;
    private final QrTokenService qrTokens;
    private final AccessControl access;
    private final Clock clock;

    public AttendanceService(AttendanceSessionDao sessionDao, AttendanceRecordDao recordDao,
                             QrTokenService qrTokens, AccessControl access, Clock clock) {
        this.sessionDao = sessionDao;
        this.recordDao = recordDao;
        this.qrTokens = qrTokens;
        this.access = access;
        this.clock = clock;
    }

    public List<AttendanceSession> listSessions() {
        access.requireRole(Role.ADMIN);
        return sessionDao.findAll();
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

    public ScanResult recordAttendance(String rawQr, VerificationMethod method) {
        User student = access.requireRole(Role.STUDENT);
        QrToken token;
        try {
            token = qrTokens.verify(rawQr);
        } catch (InvalidQrException e) {
            return ScanResult.failure("Invalid QR code", e.getMessage());
        }
        if (token.getType() != QrTokenType.ATTENDANCE) {
            return ScanResult.failure("Wrong QR code", "This is " + token.getType().getDescription()
                    + ", not an attendance QR code.");
        }
        Optional<AttendanceSession> found = sessionDao.findById(token.getSubjectId());
        if (found.isEmpty()) {
            return ScanResult.failure("Session not found", "The attendance session for this QR code no longer exists.");
        }
        AttendanceSession session = found.get();
        LocalDateTime now = now();
        if (!session.isOpenAt(now)) {
            return ScanResult.failure("Session not open", "\"" + session.getTitle() + "\" accepts attendance only from "
                    + TIME.format(session.getStartsAt()) + " to " + TIME.format(session.getEndsAt()) + ".");
        }
        if (recordDao.exists(session.getId(), student.getId())) {
            return ScanResult.failure("Already recorded", "Your attendance for \"" + session.getTitle()
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
            return ScanResult.failure("Already recorded", "Your attendance for \"" + session.getTitle()
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
