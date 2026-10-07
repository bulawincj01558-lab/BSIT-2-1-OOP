package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.DisciplineDao;
import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.CaseStatus;
import edu.liceo.ugoautomate.model.DisciplineRecord;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.util.Validation;
import edu.liceo.ugoautomate.util.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DisciplineService {
    public static final List<String> COMMON_OFFENSES = List.of(
            "Habitual tardiness",
            "Cutting classes",
            "Improper uniform",
            "Not wearing school ID",
            "Littering",
            "Disrespect to faculty or staff",
            "Cheating during examinations",
            "Vandalism",
            "Bullying or harassment",
            "Fighting",
            "Smoking or vaping on campus",
            "Possession of prohibited items");

    private final DisciplineDao disciplineDao;
    private final UserDao userDao;
    private final AccessControl access;
    private final Clock clock;

    public DisciplineService(DisciplineDao disciplineDao, UserDao userDao, AccessControl access, Clock clock) {
        this.disciplineDao = disciplineDao;
        this.userDao = userDao;
        this.access = access;
        this.clock = clock;
    }

    public List<DisciplineRecord> myRecord() {
        User student = access.requireRole(Role.STUDENT);
        return disciplineDao.findByStudent(student.getId());
    }

    public List<DisciplineRecord> search(CaseStatus status, String search) {
        access.requireRole(Role.ADMIN);
        return disciplineDao.search(status, search);
    }

    public long openCaseCount() {
        access.requireRole(Role.ADMIN);
        return disciplineDao.countByStatus(CaseStatus.OPEN);
    }

    public Student findStudent(String studentNumber) {
        access.requireRole(Role.ADMIN);
        return userDao.findByStudentNumber(Validation.studentNumber(studentNumber))
                .filter(Student.class::isInstance)
                .map(Student.class::cast)
                .orElseThrow(() -> new ServiceException("No student has the ID " + studentNumber.trim() + "."));
    }

    public DisciplineRecord create(String studentNumber, DisciplineRecord record) {
        User admin = access.requireRole(Role.ADMIN);
        record.setStudentId(findStudent(studentNumber).getId());
        validate(record, null);
        record.setRecordedBy(admin.getId());
        record.setRecordedAt(LocalDateTime.now(clock));
        disciplineDao.create(record);
        return record;
    }

    public DisciplineRecord update(DisciplineRecord record) {
        access.requireRole(Role.ADMIN);
        DisciplineRecord existing = get(record.getId());
        record.setStudentId(existing.getStudentId());
        validate(record, existing);
        disciplineDao.update(record);
        return record;
    }

    public void delete(long id) {
        access.requireRole(Role.ADMIN);
        get(id);
        disciplineDao.delete(id);
    }

    private DisciplineRecord get(long id) {
        return disciplineDao.findById(id).orElseThrow(() -> new ServiceException("Discipline record not found."));
    }

    private void validate(DisciplineRecord r, DisciplineRecord existing) {
        if (r.getIncidentDate() == null) {
            throw new ValidationException("Choose the date of the incident.");
        }
        if (r.getIncidentDate().isAfter(LocalDate.now(clock))) {
            throw new ValidationException("The incident date cannot be in the future.");
        }
        r.setOffense(Validation.required(r.getOffense(), "Offense", 100));
        if (r.getSeverity() == null) {
            throw new ValidationException("Choose the severity of the offense.");
        }
        r.setDescription(Validation.optional(r.getDescription(), 1000, "Description"));
        r.setSanction(Validation.optional(r.getSanction(), 200, "Sanction"));
        if (r.getStatus() == null) {
            r.setStatus(CaseStatus.OPEN);
        }
        r.setResolution(Validation.optional(r.getResolution(), 1000, "Resolution notes"));
        if (r.getStatus() == CaseStatus.RESOLVED) {
            boolean wasResolved = existing != null && existing.getStatus() == CaseStatus.RESOLVED;
            r.setResolvedAt(wasResolved ? existing.getResolvedAt() : LocalDateTime.now(clock));
        } else {
            r.setResolvedAt(null);
        }
    }
}
