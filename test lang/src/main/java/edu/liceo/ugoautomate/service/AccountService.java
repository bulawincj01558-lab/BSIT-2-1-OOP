package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.PasswordHasher;
import edu.liceo.ugoautomate.util.Validation;

import java.util.List;

public class AccountService {
    private final UserDao userDao;
    private final PasswordHasher hasher;
    private final AccessControl access;
    private final AuthService auth;

    public AccountService(UserDao userDao, PasswordHasher hasher, AccessControl access, AuthService auth) {
        this.userDao = userDao;
        this.hasher = hasher;
        this.access = access;
        this.auth = auth;
    }

    public Student addStudent(StudentRegistration form) {
        access.requireRole(Role.ADMIN);
        return auth.registerStudent(form);
    }

    public List<User> listAccounts(Role role, String search) {
        access.requireRole(Role.ADMIN);
        return userDao.findAll(role, search);
    }

    public User getAccount(long id) {
        access.requireRole(Role.ADMIN);
        return find(id);
    }

    public User updateAccount(long id, ProfileUpdate update, boolean active) {
        User admin = access.requireRole(Role.ADMIN);
        User target = find(id);
        if (target.getId() == admin.getId() && !active) {
            throw new ServiceException("You cannot deactivate your own account.");
        }
        update.applyTo(target);
        target.setActive(active);
        userDao.update(target);
        return target;
    }

    public void setActive(long id, boolean active) {
        User admin = access.requireRole(Role.ADMIN);
        if (id == admin.getId() && !active) {
            throw new ServiceException("You cannot deactivate your own account.");
        }
        find(id);
        userDao.setActive(id, active);
    }

    public void resetPassword(long id, String newPassword) {
        access.requireRole(Role.ADMIN);
        find(id);
        userDao.updatePassword(id, hasher.hash(Validation.password(newPassword)));
    }

    public void deleteAccount(long id) {
        User admin = access.requireRole(Role.ADMIN);
        if (id == admin.getId()) {
            throw new ServiceException("You cannot delete your own account.");
        }
        find(id);
        userDao.delete(id);
    }

    private User find(long id) {
        return userDao.findById(id).orElseThrow(() -> new ServiceException("Account not found."));
    }
}
