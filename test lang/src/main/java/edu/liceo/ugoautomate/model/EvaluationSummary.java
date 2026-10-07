package edu.liceo.ugoautomate.model;

import java.util.EnumMap;
import java.util.Map;

public class EvaluationSummary {
    private final Faculty faculty;
    private final int count;
    private final Map<EvaluationCriterion, Double> averages;

    public EvaluationSummary(Faculty faculty, int count, Map<EvaluationCriterion, Double> averages) {
        this.faculty = faculty;
        this.count = count;
        this.averages = averages.isEmpty()
                ? new EnumMap<>(EvaluationCriterion.class) : new EnumMap<>(averages);
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public int getCount() {
        return count;
    }

    public Double getAverage(EvaluationCriterion criterion) {
        return count == 0 ? null : averages.get(criterion);
    }

    public Double getOverall() {
        return count == 0 ? null : averages.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
}
