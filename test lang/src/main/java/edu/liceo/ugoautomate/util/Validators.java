package edu.liceo.ugoautomate.util;

import java.util.regex.Pattern;

/**
 * Input validation helpers. Each method returns the cleaned (trimmed) value or
 * throws {@link ValidationException} with a message suitable for the user.
 */
public final class Validators {

    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 72; // BCrypt only uses the first 72 bytes

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern STUDENT_NUMBER = Pattern.compile("^[A-Za-z0-9-]{4,20}$");
    private static final Pattern CONTACT = Pattern.compile("^[0-9+()\\- ]{7,20}$");

    private Validators() {
    }

    public static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(field + " must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    /** @return the trimmed value, or {@code null} if blank */
    public static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(value, field, maxLength);
    }

    public static String requireEmail(String value) {
        String email = requireText(value, "Email", 120);
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidationException("Please enter a valid email address.");
        }
        return email;
    }

    public static String requireContact(String value) {
        String contact = requireText(value, "Contact number", 20);
        if (!CONTACT.matcher(contact).matches()) {
            throw new ValidationException("Please enter a valid contact number (digits, spaces, +, -, parentheses).");
        }
        return contact;
    }

    public static String optionalContact(String value) {
        return value == null || value.isBlank() ? null : requireContact(value);
    }

    public static String requireStudentNumber(String value) {
        String number = requireText(value, "Student ID", 20);
        if (!STUDENT_NUMBER.matcher(number).matches()) {
            throw new ValidationException("Student ID may contain only letters, digits, and dashes (4-20 characters).");
        }
        return number;
    }

    public static int requireYearLevel(int yearLevel) {
        if (yearLevel < 1 || yearLevel > 6) {
            throw new ValidationException("Year level must be between 1 and 6.");
        }
        return yearLevel;
    }

    /**
     * Requires at least {@value #MIN_PASSWORD_LENGTH} characters with at least one
     * letter and one digit, and that the confirmation matches.
     */
    public static void requireStrongPassword(char[] password, char[] confirmation) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        if (password.length > MAX_PASSWORD_LENGTH) {
            throw new ValidationException("Password must be at most " + MAX_PASSWORD_LENGTH + " characters long.");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password) {
            hasLetter |= Character.isLetter(c);
            hasDigit |= Character.isDigit(c);
        }
        if (!hasLetter || !hasDigit) {
            throw new ValidationException("Password must contain at least one letter and one number.");
        }
        if (confirmation == null || !java.util.Arrays.equals(password, confirmation)) {
            throw new ValidationException("Passwords do not match.");
        }
    }
}
