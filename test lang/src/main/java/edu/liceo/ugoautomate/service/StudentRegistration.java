package edu.liceo.ugoautomate.service;

public record StudentRegistration(
        String studentNumber,
        String firstName,
        String lastName,
        String course,
        int yearLevel,
        String email,
        String contactNumber,
        String password,
        String confirmPassword) {
}
