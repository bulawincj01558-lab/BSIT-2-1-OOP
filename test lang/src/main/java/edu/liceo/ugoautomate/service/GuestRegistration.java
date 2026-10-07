package edu.liceo.ugoautomate.service;

public record GuestRegistration(
        String firstName,
        String lastName,
        String contactNumber,
        String email,
        String purposeOfVisit,
        String personToVisit,
        String password,
        String confirmPassword) {
}
