package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.FacultyDao;
import edu.liceo.ugoautomate.model.Faculty;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcFacultyDao extends AbstractJdbcDao implements FacultyDao {
    private static final String SELECT = "SELECT id, first_name, last_name, department, email, active FROM faculty";
    private static final String ORDER = " ORDER BY LOWER(last_name), LOWER(first_name)";

    public JdbcFacultyDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<Faculty> findAll(boolean activeOnly) {
        return queryList(SELECT + (activeOnly ? " WHERE active = 1" : "") + ORDER, JdbcFacultyDao::map);
    }

    @Override
    public Optional<Faculty> findById(long id) {
        return queryOne(SELECT + " WHERE id = ?", JdbcFacultyDao::map, id);
    }

    @Override
    public long create(Faculty f) {
        long id = insert("INSERT INTO faculty (first_name, last_name, department, email, active) VALUES (?, ?, ?, ?, ?)",
                f.getFirstName(), f.getLastName(), f.getDepartment(), f.getEmail(), f.isActive());
        f.setId(id);
        return id;
    }

    @Override
    public void update(Faculty f) {
        update("UPDATE faculty SET first_name = ?, last_name = ?, department = ?, email = ?, active = ? WHERE id = ?",
                f.getFirstName(), f.getLastName(), f.getDepartment(), f.getEmail(), f.isActive(), f.getId());
    }

    @Override
    public void delete(long id) {
        update("DELETE FROM faculty WHERE id = ?", id);
    }

    static Faculty map(ResultSet rs) throws SQLException {
        Faculty f = new Faculty();
        f.setId(rs.getLong("id"));
        f.setFirstName(rs.getString("first_name"));
        f.setLastName(rs.getString("last_name"));
        f.setDepartment(rs.getString("department"));
        f.setEmail(rs.getString("email"));
        f.setActive(rs.getInt("active") == 1);
        return f;
    }
}
