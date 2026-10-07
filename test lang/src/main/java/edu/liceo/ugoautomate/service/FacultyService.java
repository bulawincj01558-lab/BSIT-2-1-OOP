package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.FacultyDao;
import edu.liceo.ugoautomate.model.Faculty;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.util.Validation;

import java.util.List;

public class FacultyService {
    private final FacultyDao facultyDao;
    private final AccessControl access;

    public FacultyService(FacultyDao facultyDao, AccessControl access) {
        this.facultyDao = facultyDao;
        this.access = access;
    }

    public List<Faculty> listAll() {
        access.requireRole(Role.ADMIN);
        return facultyDao.findAll(false);
    }

    public List<Faculty> listActive() {
        access.requireLoggedIn();
        return facultyDao.findAll(true);
    }

    public Faculty get(long id) {
        access.requireLoggedIn();
        return facultyDao.findById(id).orElseThrow(() -> new ServiceException("Faculty member not found."));
    }

    public Faculty create(Faculty faculty) {
        access.requireRole(Role.ADMIN);
        validate(faculty);
        facultyDao.create(faculty);
        return faculty;
    }

    public Faculty update(Faculty faculty) {
        access.requireRole(Role.ADMIN);
        get(faculty.getId());
        validate(faculty);
        facultyDao.update(faculty);
        return faculty;
    }

    public void delete(long id) {
        access.requireRole(Role.ADMIN);
        get(id);
        facultyDao.delete(id);
    }

    private static void validate(Faculty f) {
        f.setFirstName(Validation.required(f.getFirstName(), "First name", 60));
        f.setLastName(Validation.required(f.getLastName(), "Last name", 60));
        f.setDepartment(Validation.required(f.getDepartment(), "Department", 100));
        String email = f.getEmail();
        f.setEmail(email == null || email.isBlank() ? null : Validation.email(email));
    }
}
