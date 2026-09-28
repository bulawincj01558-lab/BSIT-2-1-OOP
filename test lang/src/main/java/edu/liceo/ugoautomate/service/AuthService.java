package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.PasswordHasher;
import edu.liceo.ugoautomate.security.SessionManager;
import edu.liceo.ugoautomate.util.ValidationException;
import edu.liceo.ugoautomate.util.Validators;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Login, logout, self-registration (students and guests), and password changes.
 * Password character arrays passed in are wiped after use.
 */
public class AuthService {

    /**
     * Hash checked when the username does not exist, so a failed login takes
     * the same time whether or not the account exists (prevents user enumeration).
     */
    private static final String DUMMY_HASH = PasswordHasher.hash("timing-equalizer-password".toCharArray());

    private final UserDao userDao;
    private final SessionManager session;
    private final AccessControl access;
    private final Clock clock;

    public AuthService(UserDao userDao, SessionManager session, AccessControl access, Clock clock) {
        this.userDao = userDao;
        this.session = session;
        this.access = access;
        this.clock = clock;
    }

    /**
     * Verifies credentials with BCrypt and starts a session.
     *
     * @param username username (students use their student number, guests their email)
     * @param password password characters; wiped before returning
     * @return the authenticated user (concrete subtype)
     * @throws ServiceException if the credentials are invalid or the account is deactivated
     */
    public User login(String username, char[] password) {
        try {
            String name = Validators.requireText(username, "Username", 100);
            if (password == null || password.length == 0) {
                throw new ValidationException("Password is required.");
            }
            Optional<User> found = userDao.findByUsername(name);
            boolean matches = PasswordHasher.verify(password, found.map(User::getPasswordHash).orElse(DUMMY_HASH));
            if (found.isEmpty() || !matches) {
                throw new ServiceException("Invalid username or password.");
            }
            User user = found.get();
            if (!user.isActive()) {
                throw new ServiceException("This account has been deactivated. Please contact the campus administrator.");
            }
            session.start(user);
            return user;
        } finally {
            wipe(password);
        }
    }

    public void logout() {
        session.end();
    }

    /**
     * Registers a new student account (F-1.1). The student number becomes the username.
     * Also used by administrators when adding students.
     */
    public Student registerStudent(String studentNumber, String fullName, String course, int yearLevel,
                                   String email, String contactNumber, char[] password, char[] confirmPassword) {
        try {
            Student s = new Student();
            s.setStudentNumber(Validators.requireStudentNumber(studentNumber));
            s.setFullName(Validators.requireText(fullName, "Full name", 120));
            s.setCourse(Validators.requireText(course, "Course", 60).toUpperCase(Locale.ROOT));
            s.setYearLevel(Validators.requireYearLevel(yearLevel));
            s.setEmail(Validators.requireEmail(email));
            s.setContactNumber(Validators.optionalContact(contactNumber));
            Validators.requireStrongPassword(password, confirmPassword);

            if (userDao.studentNumberExists(s.getStudentNumber()) || userDao.usernameExists(s.getStudentNumber())) {
                throw new ServiceException("An account with student ID " + s.getStudentNumber() + " already exists.");
            }
            s.setUsername(s.getStudentNumber());
            s.setPasswordHash(PasswordHasher.hash(password));
            s.setActive(true);
            s.setCreatedAt(LocalDateTime.now(clock));
            userDao.insertStudent(s);
            return s;
        } finally {
            wipe(password);
            wipe(confirmPassword);
        }
    }

    /**
     * Registers a new guest account (F-1.2) capturing the information required
     * before campus entry. The email address becomes the username.
     */
    public Guest registerGuest(String fullName, String contactNumber, String email, String purposeOfVisit,
                               String personToVisit, char[] password, char[] confirmPassword) {
        try {
            Guest g = new Guest();
            g.setFullName(Validators.requireText(fullName, "Full name", 120));
            g.setContactNumber(Validators.requireContact(contactNumber));
            g.setEmail(Validators.requireEmail(email).toLowerCase(Locale.ROOT));
            g.setPurposeOfVisit(Validators.requireText(purposeOfVisit, "Purpose of visit", 200));
            g.setPersonToVisit(Validators.requireText(personToVisit, "Person / office to visit", 120));
            Validators.requireStrongPassword(password, confirmPassword);

            if (userDao.usernameExists(g.getEmail())) {
                throw new ServiceException("An account with email " + g.getEmail() + " already exists.");
            }
            g.setUsername(g.getEmail());
            g.setPasswordHash(PasswordHasher.hash(password));
            g.setActive(true);
            g.setCreatedAt(LocalDateTime.now(clock));
            userDao.insertGuest(g);
            return g;
        } finally {
            wipe(password);
            wipe(confirmPassword);
        }
    }

    /**
     * Changes the signed-in user's password after re-verifying the current one.
     */
    public void changePassword(char[] currentPassword, char[] newPassword, char[] confirmPassword) {
        try {
            User user = access.requireAuthenticated();
            if (!PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
                throw new ServiceException("Your current password is incorrect.");
            }
            Validators.requireStrongPassword(newPassword, confirmPassword);
            userDao.updatePasswordHash(user.getId(), PasswordHasher.hash(newPassword));
        } finally {
            wipe(currentPassword);
            wipe(newPassword);
            wipe(confirmPassword);
        }
    }

    static void wipe(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, '\0');
        }
    }
}
