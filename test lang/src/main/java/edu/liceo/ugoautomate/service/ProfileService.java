package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.PasswordHasher;
import edu.liceo.ugoautomate.security.SessionManager;
import edu.liceo.ugoautomate.util.Validation;
import edu.liceo.ugoautomate.util.ValidationException;

public class ProfileService {
    private final UserDao userDao;
    private final PasswordHasher hasher;
    private final SessionManager session;
    private final AccessControl access;

    public ProfileService(UserDao userDao, PasswordHasher hasher, SessionManager session, AccessControl access) {
        this.userDao = userDao;
        this.hasher = hasher;
        this.session = session;
        this.access = access;
    }

    public User myProfile() {
        User me = access.requireLoggedIn();
        return userDao.findById(me.getId())
                .orElseThrow(() -> new ServiceException("Your account no longer exists."));
    }

    public User updateMyProfile(ProfileUpdate update) {
        User current = myProfile();
        update.applyTo(current);
        userDao.update(current);
        session.refresh(current);
        return current;
    }

    public void changeMyPassword(String currentPassword, String newPassword, String confirmPassword) {
        User current = myProfile();
        if (currentPassword == null || !hasher.verify(currentPassword, current.getPasswordHash())) {
            throw new ServiceException("Your current password is incorrect.");
        }
        Validation.password(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("New passwords do not match.");
        }
        userDao.updatePassword(current.getId(), hasher.hash(newPassword));
    }
}
