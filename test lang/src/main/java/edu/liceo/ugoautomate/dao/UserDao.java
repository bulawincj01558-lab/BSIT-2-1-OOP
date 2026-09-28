package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.Administrator;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Persistence for all account types. Returned {@link User} instances are the
 * concrete subtype matching the stored role.
 */
public interface UserDao {

    Optional<User> findById(long id);

    Optional<User> findByUsername(String username);

    Optional<Student> findStudentByNumber(String studentNumber);

    List<Student> findAllStudents();

    List<Guest> findAllGuests();

    boolean usernameExists(String username);

    boolean studentNumberExists(String studentNumber);

    long countByRole(Role role);

    /** Inserts the user and student rows atomically; sets the generated id on {@code student}. */
    long insertStudent(Student student);

    /** Inserts the user and guest rows atomically; sets the generated id on {@code guest}. */
    long insertGuest(Guest guest);

    /** Inserts the user and administrator rows atomically; sets the generated id. */
    long insertAdministrator(Administrator admin);

    /** Updates base profile fields plus the subtype fields of the given user. */
    void updateProfile(User user);

    void updatePasswordHash(long userId, String passwordHash);

    void setActive(long userId, boolean active);

    boolean delete(long userId);
}
