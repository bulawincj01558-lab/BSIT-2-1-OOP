package edu.liceo.ugoautomate.model;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public class FacultyEvaluation {
    private long id;
    private long facultyId;
    private String facultyName;
    private long studentId;
    private String term;
    private final Map<EvaluationCriterion, Integer> ratings = new EnumMap<>(EvaluationCriterion.class);
    private String comment;
    private LocalDateTime submittedAt;

    public double getAverage() {
        return ratings.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(long facultyId) {
        this.facultyId = facultyId;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public long getStudentId() {
        return studentId;
    }

    public void setStudentId(long studentId) {
        this.studentId = studentId;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public Integer getRating(EvaluationCriterion criterion) {
        return ratings.get(criterion);
    }

    public void setRating(EvaluationCriterion criterion, int rating) {
        ratings.put(criterion, rating);
    }

    public Map<EvaluationCriterion, Integer> getRatings() {
        return new EnumMap<>(ratings);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
