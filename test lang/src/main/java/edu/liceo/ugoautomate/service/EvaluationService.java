package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.ClassOfferingDao;
import edu.liceo.ugoautomate.dao.DataIntegrityException;
import edu.liceo.ugoautomate.dao.FacultyDao;
import edu.liceo.ugoautomate.dao.FacultyEvaluationDao;
import edu.liceo.ugoautomate.dao.SettingsDao;
import edu.liceo.ugoautomate.model.EvaluationCriterion;
import edu.liceo.ugoautomate.model.EvaluationPeriod;
import edu.liceo.ugoautomate.model.EvaluationSummary;
import edu.liceo.ugoautomate.model.Faculty;
import edu.liceo.ugoautomate.model.FacultyEvaluation;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.util.Validation;
import edu.liceo.ugoautomate.util.ValidationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EvaluationService {
    static final String OPEN_KEY = "evaluation.open";
    private static final int MAX_COMMENT = 1000;

    private final FacultyDao facultyDao;
    private final FacultyEvaluationDao evaluationDao;
    private final ClassOfferingDao classDao;
    private final SettingsDao settings;
    private final AcademicTerm term;
    private final AccessControl access;
    private final Clock clock;

    public EvaluationService(FacultyDao facultyDao, FacultyEvaluationDao evaluationDao, ClassOfferingDao classDao,
                             SettingsDao settings, AcademicTerm term, AccessControl access, Clock clock) {
        this.facultyDao = facultyDao;
        this.evaluationDao = evaluationDao;
        this.classDao = classDao;
        this.settings = settings;
        this.term = term;
        this.access = access;
        this.clock = clock;
    }

    public EvaluationPeriod currentPeriod() {
        access.requireLoggedIn();
        boolean open = settings.get(OPEN_KEY).map(Boolean::parseBoolean).orElse(true);
        return new EvaluationPeriod(term.current(), open);
    }

    public EvaluationPeriod setPeriod(String newTerm, boolean open) {
        access.requireRole(Role.ADMIN);
        String cleaned = Validation.required(newTerm, "Term", 60);
        term.set(cleaned);
        settings.put(OPEN_KEY, Boolean.toString(open));
        return new EvaluationPeriod(cleaned, open);
    }

    public List<Faculty> facultyToEvaluate() {
        User student = access.requireRole(Role.STUDENT);
        return allowedFaculty(student.getId());
    }

    private List<Faculty> allowedFaculty(long studentId) {
        List<Faculty> active = facultyDao.findAll(true);
        Set<Long> teachers = classDao.findFacultyIdsOfStudent(studentId, term.current());
        return teachers.isEmpty() ? active : active.stream().filter(f -> teachers.contains(f.getId())).toList();
    }

    public Set<Long> myEvaluatedFacultyIds() {
        User student = access.requireRole(Role.STUDENT);
        return evaluationDao.findByStudent(student.getId(), currentPeriod().term()).stream()
                .map(FacultyEvaluation::getFacultyId)
                .collect(Collectors.toSet());
    }

    public FacultyEvaluation submit(long facultyId, Map<EvaluationCriterion, Integer> ratings, String comment) {
        User student = access.requireRole(Role.STUDENT);
        EvaluationPeriod period = currentPeriod();
        if (!period.open()) {
            throw new ServiceException("Faculty evaluation for " + period.term() + " is closed.");
        }
        Faculty faculty = allowedFaculty(student.getId()).stream()
                .filter(f -> f.getId() == facultyId)
                .findFirst()
                .orElseThrow(() -> new ServiceException("You can only evaluate the active instructors of your classes."));

        FacultyEvaluation evaluation = new FacultyEvaluation();
        evaluation.setFacultyId(faculty.getId());
        evaluation.setFacultyName(faculty.getFullName());
        evaluation.setStudentId(student.getId());
        evaluation.setTerm(period.term());
        for (EvaluationCriterion criterion : EvaluationCriterion.values()) {
            Integer rating = ratings.get(criterion);
            if (rating == null) {
                throw new ValidationException("Please rate: " + criterion.getQuestion() + ".");
            }
            evaluation.setRating(criterion, Validation.range(rating,
                    EvaluationCriterion.MIN_RATING, EvaluationCriterion.MAX_RATING, criterion.getQuestion()));
        }
        evaluation.setComment(Validation.optional(comment, MAX_COMMENT, "Comment"));
        evaluation.setSubmittedAt(LocalDateTime.now(clock));

        if (evaluationDao.exists(facultyId, student.getId(), period.term())) {
            throw new ServiceException("You already evaluated " + faculty.getFullName() + " for " + period.term() + ".");
        }
        try {
            evaluationDao.create(evaluation);
        } catch (DataIntegrityException e) {
            throw new ServiceException("You already evaluated " + faculty.getFullName() + " for " + period.term() + ".");
        }
        return evaluation;
    }

    public List<EvaluationSummary> summaries(String term) {
        access.requireRole(Role.ADMIN);
        return evaluationDao.summarize(term);
    }

    public List<FacultyEvaluation> evaluationsOf(long facultyId, String term) {
        access.requireRole(Role.ADMIN);
        List<FacultyEvaluation> evaluations = evaluationDao.findByFaculty(facultyId, term);
        evaluations.forEach(e -> e.setStudentId(0));
        return evaluations;
    }

    public List<String> terms() {
        access.requireRole(Role.ADMIN);
        String current = currentPeriod().term();
        List<String> terms = new ArrayList<>(List.of(current));
        evaluationDao.findTerms().stream().filter(t -> !t.equals(current)).forEach(terms::add);
        return terms;
    }
}
