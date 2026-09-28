package edu.liceo.ugoautomate.security;

import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;

/**
 * Role-based authorization used by every protected service method.
 * <p>
 * Each check re-reads the account by primary key, so an account deactivated
 * by an administrator loses access immediately, not only at next login.
 * The re-read is an indexed single-row lookup.
 */
public final class AccessControl {

    private final SessionManager session;
    private final UserDao userDao;

    public AccessControl(SessionManager session, UserDao userDao) {
        this.session = session;
        this.userDao = userDao;
    }

    /**
     * @return the up-to-date signed-in user
     * @throws AccessDeniedException if nobody is signed in or the account was deactivated/removed
     */
    public User requireAuthenticated() {
        User current = session.currentUser()
                .orElseThrow(() -> new AccessDeniedException("Please log in to continue."));
        User fresh = userDao.findById(current.getId()).orElse(null);
        if (fresh == null || !fresh.isActive()) {
            session.end();
            throw new AccessDeniedException("Your account is no longer active. Please contact the administrator.");
        }
        return fresh;
    }

    /**
     * @param allowed roles permitted to perform the action
     * @return the signed-in user if their role is in {@code allowed}
     */
    public User requireRole(Role... allowed) {
        User user = requireAuthenticated();
        for (Role role : allowed) {
            if (user.getRole() == role) {
                return user;
            }
        }
        throw new AccessDeniedException("You do not have permission to use this feature.");
    }

    public Administrator requireAdmin() {
        return (Administrator) requireRole(Role.ADMIN);
    }

    public Student requireStudent() {
        return (Student) requireRole(Role.STUDENT);
    }

    public Guest requireGuest() {
        return (Guest) requireRole(Role.GUEST);
    }
}
