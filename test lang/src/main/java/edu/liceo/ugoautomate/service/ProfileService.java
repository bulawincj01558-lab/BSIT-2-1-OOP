package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.SessionManager;
import edu.liceo.ugoautomate.util.Validators;

import java.util.Locale;

/**
 * Viewing and updating the signed-in user's own profile (F-1.4).
 * <p>
 * Permitted fields by role:
 * <ul>
 *   <li>Student: email, contact number, course, year level. Name and student ID
 *       are official records and can only be changed by an administrator.</li>
 *   <li>Guest: full name, email, contact number, default purpose of visit,
 *       default person/office to visit. The username stays unchanged.</li>
 *   <li>Administrator: full name, email, contact number, office.</li>
 * </ul>
 */
public class ProfileService {

    private final UserDao userDao;
    private final AccessControl access;
    private final SessionManager session;

    public ProfileService(UserDao userDao, AccessControl access, SessionManager session) {
        this.userDao = userDao;
        this.access = access;
        this.session = session;
    }

    /** @return a fresh copy of the signed-in user's profile */
    public User getMyProfile() {
        return access.requireAuthenticated();
    }

    /**
     * Applies the permitted fields of {@code update} to the signed-in user's profile.
     *
     * @return the updated profile
     */
    public User updateMyProfile(ProfileUpdate update) {
        User user = access.requireAuthenticated();

        if (user instanceof Student s) {
            if (update.getEmail() != null) {
                s.setEmail(Validators.requireEmail(update.getEmail()));
            }
            if (update.getContactNumber() != null) {
                s.setContactNumber(Validators.optionalContact(update.getContactNumber()));
            }
            if (update.getCourse() != null) {
                s.setCourse(Validators.requireText(update.getCourse(), "Course", 60).toUpperCase(Locale.ROOT));
            }
            if (update.getYearLevel() != null) {
                s.setYearLevel(Validators.requireYearLevel(update.getYearLevel()));
            }
        } else if (user instanceof Guest g) {
            if (update.getFullName() != null) {
                g.setFullName(Validators.requireText(update.getFullName(), "Full name", 120));
            }
            if (update.getEmail() != null) {
                g.setEmail(Validators.requireEmail(update.getEmail()));
            }
            if (update.getContactNumber() != null) {
                g.setContactNumber(Validators.requireContact(update.getContactNumber()));
            }
            if (update.getPurposeOfVisit() != null) {
                g.setPurposeOfVisit(Validators.requireText(update.getPurposeOfVisit(), "Purpose of visit", 200));
            }
            if (update.getPersonToVisit() != null) {
                g.setPersonToVisit(Validators.requireText(update.getPersonToVisit(), "Person / office to visit", 120));
            }
        } else if (user instanceof Administrator a) {
            if (update.getFullName() != null) {
                a.setFullName(Validators.requireText(update.getFullName(), "Full name", 120));
            }
            if (update.getEmail() != null) {
                a.setEmail(Validators.requireEmail(update.getEmail()));
            }
            if (update.getContactNumber() != null) {
                a.setContactNumber(Validators.optionalContact(update.getContactNumber()));
            }
            if (update.getOffice() != null) {
                a.setOffice(Validators.requireText(update.getOffice(), "Office", 120));
            }
        }

        userDao.updateProfile(user);
        session.start(user);
        return user;
    }
}
