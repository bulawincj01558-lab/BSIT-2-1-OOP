package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.util.Validation;

public record ProfileUpdate(
        String firstName,
        String lastName,
        String contactNumber,
        String course,
        Integer yearLevel,
        String purposeOfVisit,
        String personToVisit,
        String position) {
    public static ProfileUpdate of(User user) {
        String course = null;
        Integer year = null;
        String purpose = null;
        String person = null;
        String position = null;
        if (user instanceof Student s) {
            course = s.getCourse();
            year = s.getYearLevel();
        } else if (user instanceof Guest g) {
            purpose = g.getPurposeOfVisit();
            person = g.getPersonToVisit();
        } else if (user instanceof Administrator a) {
            position = a.getPosition();
        }
        return new ProfileUpdate(user.getFirstName(), user.getLastName(), user.getContactNumber(),
                course, year, purpose, person, position);
    }

    public void applyTo(User user) {
        user.setFirstName(Validation.required(firstName, "First name", 60));
        user.setLastName(Validation.required(lastName, "Last name", 60));
        boolean contactRequired = user instanceof Guest;
        user.setContactNumber(Validation.contactNumber(contactNumber, contactRequired));
        if (user instanceof Student s) {
            s.setCourse(Validation.required(course, "Course", 100));
            s.setYearLevel(Validation.yearLevel(yearLevel == null ? 0 : yearLevel));
        } else if (user instanceof Guest g) {
            g.setPurposeOfVisit(Validation.required(purposeOfVisit, "Purpose of visit", 200));
            g.setPersonToVisit(Validation.required(personToVisit, "Person/office to visit", 100));
        } else if (user instanceof Administrator a) {
            a.setPosition(Validation.optional(position, 100, "Position"));
        }
    }
}
