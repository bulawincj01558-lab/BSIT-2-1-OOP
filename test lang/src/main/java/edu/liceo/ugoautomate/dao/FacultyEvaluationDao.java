package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.EvaluationSummary;
import edu.liceo.ugoautomate.model.FacultyEvaluation;

import java.util.List;

public interface FacultyEvaluationDao {
    long create(FacultyEvaluation evaluation);

    boolean exists(long facultyId, long studentId, String term);

    List<FacultyEvaluation> findByStudent(long studentId, String term);

    List<EvaluationSummary> summarize(String term);

    List<FacultyEvaluation> findByFaculty(long facultyId, String term);

    List<String> findTerms();
}
