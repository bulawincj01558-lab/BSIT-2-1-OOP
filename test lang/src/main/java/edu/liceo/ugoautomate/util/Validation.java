package edu.liceo.ugoautomate.util;

import java.util.regex.Pattern;

public final class Validation {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern STUDENT_NUMBER = Pattern.compile("^\\d{4}-\\d{5}$");
    private static final Pattern CONTACT = Pattern.compile("^[0-9+()\\- ]{7,20}$");

    public static final int MIN_PASSWORD_LENGTH = 8;

    private Validation() {
    }

    public static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required.");
        }
        return value.trim();
    }

    public static String required(String value, String field, int maxLength) {
        String cleaned = required(value, field);
        if (cleaned.length() > maxLength) {
            throw new ValidationException(field + " must be at most " + maxLength + " characters.");
        }
        return cleaned;
    }

    public static String optional(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.length() > maxLength) {
            throw new ValidationException(field + " must be at most " + maxLength + " characters.");
        }
        return cleaned;
    }

    public static String email(String value) {
        String cleaned = required(value, "Email", 120).toLowerCase();
        if (!EMAIL.matcher(cleaned).matches()) {
            throw new ValidationException("Enter a valid email address.");
        }
        return cleaned;
    }

    public static String studentNumber(String value) {
        String cleaned = required(value, "Student ID");
        if (!STUDENT_NUMBER.matcher(cleaned).matches()) {
            throw new ValidationException("Student ID must look like 2023-00123.");
        }
        return cleaned;
    }

    public static String contactNumber(String value, boolean requiredField) {
        if (!requiredField && (value == null || value.isBlank())) {
            return null;
        }
        String cleaned = required(value, "Contact number");
        if (!CONTACT.matcher(cleaned).matches()) {
            throw new ValidationException("Enter a valid contact number (digits, spaces, + - ( ) only).");
        }
        return cleaned;
    }

    public static int yearLevel(int value) {
        if (value < 1 || value > 6) {
            throw new ValidationException("Year level must be between 1 and 6.");
        }
        return value;
    }

    public static String password(String value) {
        if (value == null || value.length() < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new ValidationException("Password must contain at least one letter and one number.");
        }
        return value;
    }

    public static int range(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new ValidationException(field + " must be between " + min + " and " + max + ".");
        }
        return value;
    }
}
