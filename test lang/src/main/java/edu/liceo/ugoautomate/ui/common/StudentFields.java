package edu.liceo.ugoautomate.ui.common;

import javafx.scene.control.ComboBox;

public final class StudentFields {
    public static final String[] COURSES = {
            "BS Information Technology",
            "BS Computer Science",
            "BS Computer Engineering",
            "BS Civil Engineering",
            "BS Accountancy",
            "BS Business Administration",
            "BS Nursing",
            "BS Psychology",
            "BS Criminology",
            "Bachelor of Elementary Education",
            "Bachelor of Secondary Education"
    };

    private StudentFields() {
    }

    public static void setupCourse(ComboBox<String> course) {
        course.getItems().setAll(COURSES);
        course.setEditable(true);
    }

    public static void setupYearLevel(ComboBox<Integer> yearLevel) {
        yearLevel.getItems().setAll(1, 2, 3, 4, 5, 6);
        yearLevel.getSelectionModel().selectFirst();
    }

    public static String courseText(ComboBox<String> course) {
        String typed = course.getEditor().getText();
        return typed == null ? "" : typed;
    }

    public static void setCourse(ComboBox<String> course, String value) {
        course.setValue(value);
        course.getEditor().setText(value == null ? "" : value);
    }
}
