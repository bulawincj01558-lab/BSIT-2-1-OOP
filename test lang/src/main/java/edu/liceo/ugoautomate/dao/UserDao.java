package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {
    Optional<User> findById(long id);

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentNumber(String studentNumber);

    List<User> findAll(Role role, String search);

    boolean emailExists(String email);

    boolean studentNumberExists(String studentNumber);

    long create(User user);

    void update(User user);

    void updatePassword(long userId, String passwordHash);

    void setActive(long userId, boolean active);

    void delete(long userId);
}
