package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.DataIntegrityException;
import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.PasswordHasher;
import edu.liceo.ugoautomate.security.SessionManager;
import edu.liceo.ugoautomate.util.Validation;
import edu.liceo.ugoautomate.util.ValidationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

public class AuthService {
    private static final String INVALID_LOGIN = "Invalid email or password.";

    private final UserDao userDao;
    private final PasswordHasher hasher;
    private final SessionManager session;
    private final Clock clock;

    public AuthService(UserDao userDao, PasswordHasher hasher, SessionManager session, Clock clock) {
        this.userDao = userDao;
        this.hasher = hasher;
        this.session = session;
        this.clock = clock;
    }

    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new ServiceException("Enter your email and password.");
        }
        Optional<User> found = userDao.findByEmail(email.trim().toLowerCase());
        if (found.isEmpty()) {
            hasher.verify(password, hasher.dummyHash());
            throw new ServiceException(INVALID_LOGIN);
        }
        User user = found.get();
        if (!hasher.verify(password, user.getPasswordHash())) {
            throw new ServiceException(INVALID_LOGIN);
        }
        if (!user.isActive()) {
            throw new ServiceException("This account has been deactivated. Please contact the administrator.");
        }
        session.start(user);
        return user;
    }

    public void logout() {
        session.end();
    }

    public Student registerStudent(StudentRegistration form) {
        Student student = new Student();
        student.setStudentNumber(Validation.studentNumber(form.studentNumber()));
        student.setFirstName(Validation.required(form.firstName(), "First name", 60));
        student.setLastName(Validation.required(form.lastName(), "Last name", 60));
        student.setCourse(Validation.required(form.course(), "Course", 100));
        student.setYearLevel(Validation.yearLevel(form.yearLevel()));
        student.setEmail(Validation.email(form.email()));
        student.setContactNumber(Validation.contactNumber(form.contactNumber(), false));
        student.setPasswordHash(hasher.hash(checkPasswords(form.password(), form.confirmPassword())));

        if (userDao.studentNumberExists(student.getStudentNumber())) {
            throw new ServiceException("Student ID " + student.getStudentNumber() + " is already registered.");
        }
        return create(student);
    }

    public Guest registerGuest(GuestRegistration form) {
        Guest guest = new Guest();
        guest.setFirstName(Validation.required(form.firstName(), "First name", 60));
        guest.setLastName(Validation.required(form.lastName(), "Last name", 60));
        guest.setContactNumber(Validation.contactNumber(form.contactNumber(), true));
        guest.setEmail(Validation.email(form.email()));
        guest.setPurposeOfVisit(Validation.required(form.purposeOfVisit(), "Purpose of visit", 200));
        guest.setPersonToVisit(Validation.required(form.personToVisit(), "Person/office to visit", 100));
        guest.setPasswordHash(hasher.hash(checkPasswords(form.password(), form.confirmPassword())));
        return create(guest);
    }

    private static String checkPasswords(String password, String confirm) {
        Validation.password(password);
        if (!password.equals(confirm)) {
            throw new ValidationException("Passwords do not match.");
        }
        return password;
    }

    private <T extends User> T create(T user) {
        if (userDao.emailExists(user.getEmail())) {
            throw new ServiceException("The email " + user.getEmail() + " is already registered.");
        }
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now(clock).withNano(0));
        try {
            userDao.create(user);
        } catch (DataIntegrityException e) {
            throw new ServiceException("That email or student ID is already registered.", e);
        }
        return user;
    }
}
