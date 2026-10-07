package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.model.EnrolledStudent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ClassOfferingDao {
    List<ClassOffering> findByTerm(String term);

    Optional<ClassOffering> findById(long id);

    List<ClassOffering> findByStudent(long studentId, String term);

    List<String> findTerms();

    long create(ClassOffering offering);

    void update(ClassOffering offering);

    void delete(long id);

    List<EnrolledStudent> findEnrolled(long classId);

    boolean enroll(long classId, long studentId);

    void unenroll(long classId, long studentId);

    List<EnrolledStudent> findStudents(String course, int yearLevel);

    Optional<EnrolledStudent> findStudentByNumber(String studentNumber);

    Set<Long> findFacultyIdsOfStudent(long studentId, String term);
}
