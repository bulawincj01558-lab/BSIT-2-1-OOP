package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.ClassOfferingDao;
import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.model.EnrolledStudent;
import edu.liceo.ugoautomate.model.EnrollmentResult;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.util.Validation;
import edu.liceo.ugoautomate.util.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClassProgramService {
    private final ClassOfferingDao classDao;
    private final AcademicTerm term;
    private final AccessControl access;

    public ClassProgramService(ClassOfferingDao classDao, AcademicTerm term, AccessControl access) {
        this.classDao = classDao;
        this.term = term;
        this.access = access;
    }

    public String currentTerm() {
        access.requireLoggedIn();
        return term.current();
    }

    public List<ClassOffering> myClassProgram() {
        User student = access.requireRole(Role.STUDENT);
        return classDao.findByStudent(student.getId(), term.current());
    }

    public List<String> terms() {
        access.requireRole(Role.ADMIN);
        String current = term.current();
        List<String> terms = new ArrayList<>(List.of(current));
        classDao.findTerms().stream().filter(t -> !t.equals(current)).forEach(terms::add);
        return terms;
    }

    public List<ClassOffering> listClasses(String forTerm) {
        access.requireRole(Role.ADMIN);
        return classDao.findByTerm(forTerm);
    }

    public ClassOffering create(ClassOffering offering) {
        access.requireRole(Role.ADMIN);
        validate(offering);
        classDao.create(offering);
        return offering;
    }

    public ClassOffering update(ClassOffering offering) {
        access.requireRole(Role.ADMIN);
        get(offering.getId());
        validate(offering);
        for (EnrolledStudent student : classDao.findEnrolled(offering.getId())) {
            conflictFor(student.userId(), offering).ifPresent(other -> {
                throw new ServiceException("The new schedule conflicts with " + other + " of "
                        + student.name() + " (" + other.getScheduleText() + ").");
            });
        }
        classDao.update(offering);
        return offering;
    }

    public void delete(long classId) {
        access.requireRole(Role.ADMIN);
        get(classId);
        classDao.delete(classId);
    }

    public List<EnrolledStudent> roster(long classId) {
        access.requireRole(Role.ADMIN);
        return classDao.findEnrolled(classId);
    }

    public EnrolledStudent enrollByStudentNumber(long classId, String studentNumber) {
        access.requireRole(Role.ADMIN);
        ClassOffering offering = get(classId);
        EnrolledStudent student = classDao.findStudentByNumber(Validation.studentNumber(studentNumber))
                .orElseThrow(() -> new ServiceException("No student has the ID " + studentNumber.trim() + "."));
        conflictFor(student.userId(), offering).ifPresent(other -> {
            throw new ServiceException(student.name() + " already has " + other + " at that time ("
                    + other.getScheduleText() + ").");
        });
        if (!classDao.enroll(classId, student.userId())) {
            throw new ServiceException(student.name() + " is already enrolled in " + offering + ".");
        }
        return student;
    }

    public EnrollmentResult enrollCourse(long classId, String course, int yearLevel) {
        access.requireRole(Role.ADMIN);
        ClassOffering offering = get(classId);
        String cleanedCourse = Validation.required(course, "Course", 100);
        int enrolled = 0;
        int already = 0;
        List<String> conflicts = new ArrayList<>();
        for (EnrolledStudent student : classDao.findStudents(cleanedCourse, Validation.yearLevel(yearLevel))) {
            Optional<ClassOffering> clash = conflictFor(student.userId(), offering);
            if (clash.isPresent()) {
                conflicts.add(student.name() + " - " + clash.get() + " (" + clash.get().getScheduleText() + ")");
            } else if (classDao.enroll(classId, student.userId())) {
                enrolled++;
            } else {
                already++;
            }
        }
        return new EnrollmentResult(enrolled, already, conflicts);
    }

    public void unenroll(long classId, long studentId) {
        access.requireRole(Role.ADMIN);
        classDao.unenroll(classId, studentId);
    }

    private ClassOffering get(long classId) {
        return classDao.findById(classId).orElseThrow(() -> new ServiceException("Class not found."));
    }

    private Optional<ClassOffering> conflictFor(long studentId, ClassOffering offering) {
        return classDao.findByStudent(studentId, offering.getTerm()).stream()
                .filter(other -> other.getId() != offering.getId())
                .filter(offering::conflictsWith)
                .findFirst();
    }

    private void validate(ClassOffering c) {
        c.setTerm(Validation.required(c.getTerm(), "Term", 60));
        c.setSubjectCode(Validation.required(c.getSubjectCode(), "Subject code", 20).toUpperCase());
        c.setSubjectTitle(Validation.required(c.getSubjectTitle(), "Subject title", 120));
        c.setSection(Validation.required(c.getSection(), "Section", 30));
        c.setUnits(Validation.range(c.getUnits(), 0, 10, "Units"));
        c.setRoom(Validation.optional(c.getRoom(), 60, "Room"));
        if (c.getDays().isEmpty()) {
            throw new ValidationException("Choose at least one day.");
        }
        if (c.getStartTime() == null || c.getEndTime() == null || !c.getStartTime().isBefore(c.getEndTime())) {
            throw new ValidationException("The end time must be after the start time.");
        }
    }
}
