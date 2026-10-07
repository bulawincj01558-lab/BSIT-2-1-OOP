package edu.liceo.ugoautomate.model;

public enum EvaluationCriterion {
    KNOWLEDGE("knowledge", "Knowledge", "Knowledge of the subject matter"),
    CLARITY("clarity", "Clarity", "Clarity of explanations and teaching methods"),
    MANAGEMENT("management", "Management", "Classroom management and use of class time"),
    FAIRNESS("fairness", "Fairness", "Fairness in grading and giving feedback"),
    PUNCTUALITY("punctuality", "Punctuality", "Punctuality and availability for consultation");

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    private final String column;
    private final String shortLabel;
    private final String question;

    EvaluationCriterion(String column, String shortLabel, String question) {
        this.column = column;
        this.shortLabel = shortLabel;
        this.question = question;
    }

    public String getColumn() {
        return column;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getQuestion() {
        return question;
    }

    public static String ratingLabel(int rating) {
        return switch (rating) {
            case 1 -> "Poor";
            case 2 -> "Fair";
            case 3 -> "Good";
            case 4 -> "Very good";
            case 5 -> "Excellent";
            default -> "";
        };
    }

    @Override
    public String toString() {
        return question;
    }
}
