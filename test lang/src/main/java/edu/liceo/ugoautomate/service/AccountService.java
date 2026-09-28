package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.PasswordHasher;
import edu.liceo.ugoautomate.util.Validators;

import java.util.List;
import java.util.Locale;

/**
 * Administrator management of student and guest accounts (F-5.1).
 * Every method requires an administrator session. Administrators cannot
 * modify other administrator accounts through this service.
 */
public class AccountService {

    private final UserDao userDao;
    private final AccessControl access;

    public AccountService(UserDao userDao, AccessControl access) {
        this.userDao = userDao;
        this.access = access;
    }

    public List<Student> listStudents() {
        access.requireAdmin();
        return userDao.findAllStudents();
    }

    public List<Guest> listGuests() {
        access.requireAdmin();
        return userDao.findAllGuests();
    }

    public void setActive(long userId, boolean active) {
        access.requireAdmin();
        managedAccount(userId);
        userDao.setActive(userId, active);
    }

    public void deleteAccount(long userId) {
        access.requireAdmin();
        managedAccount(userId);
        userDao.delete(userId);
    }

    public void resetPassword(long userId, char[] newPassword, char[] confirmPassword) {
        try {
            access.requireAdmin();
            managedAccount(userId);
            Validators.requireStrongPassword(newPassword, confirmPassword);
            userDao.updatePasswordHash(userId, PasswordHasher.hash(newPassword));
        } finally {
            AuthService.wipe(newPassword);
            AuthService.wipe(confirmPassword);
        }
    }

    public Student updateStudent(long userId, String fullName, String email, String contactNumber,
                                 String course, int yearLevel) {
        access.requireAdmin();
        if (!(managedAccount(userId) instanceof Student s)) {
            throw new ServiceException("The selected account is not a student account.");
        }
        s.setFullName(Validators.requireText(fullName, "Full name", 120));
        s.setEmail(Validators.requireEmail(email));
        s.setContactNumber(Validators.optionalContact(contactNumber));
        s.setCourse(Validators.requireText(course, "Course", 60).toUpperCase(Locale.ROOT));
        s.setYearLevel(Validators.requireYearLevel(yearLevel));
        userDao.updateProfile(s);
        return s;
    }

    public Guest updateGuest(long userId, String fullName, String email, String contactNumber,
                             String purposeOfVisit, String personToVisit) {
        access.requireAdmin();
        if (!(managedAccount(userId) instanceof Guest g)) {
            throw new ServiceException("The selected account is not a guest account.");
        }
        g.setFullName(Validators.requireText(fullName, "Full name", 120));
        g.setEmail(Validators.requireEmail(email));
        g.setContactNumber(Validators.requireContact(contactNumber));
        g.setPurposeOfVisit(Validators.requireText(purposeOfVisit, "Purpose of visit", 200));
        g.setPersonToVisit(Validators.requireText(personToVisit, "Person / office to visit", 120));
        userDao.updateProfile(g);
        return g;
    }

    /** Loads a student or guest account, rejecting unknown ids and administrator accounts. */
    private User managedAccount(long userId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ServiceException("The selected account no longer exists."));
        if (user instanceof Administrator) {
            throw new ServiceException("Administrator accounts cannot be managed from this screen.");
        }
        return user;
    }
}
