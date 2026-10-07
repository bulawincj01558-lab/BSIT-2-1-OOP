package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.LocationDao;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.LocationType;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcLocationDao extends AbstractJdbcDao implements LocationDao {
    private static final String SELECT = """
            SELECT id, name, type, building, floor, description
            FROM locations
            """;
    private static final String ORDER = " ORDER BY LOWER(name)";

    public JdbcLocationDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<CampusLocation> findAll() {
        return queryList(SELECT + ORDER, JdbcLocationDao::map);
    }

    @Override
    public List<CampusLocation> search(String text, LocationType type) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (type != null) {
            sql.append(" AND type = ?");
            params.add(type);
        }
        if (text != null && !text.isBlank()) {
            String pattern = containsPattern(text);
            sql.append(" AND (LOWER(name) LIKE ? ESCAPE '\\' OR LOWER(building) LIKE ? ESCAPE '\\'"
                    + " OR LOWER(description) LIKE ? ESCAPE '\\')");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(ORDER);
        return queryList(sql.toString(), JdbcLocationDao::map, params.toArray());
    }

    @Override
    public Optional<CampusLocation> findById(long id) {
        return queryOne(SELECT + " WHERE id = ?", JdbcLocationDao::map, id);
    }

    @Override
    public long create(CampusLocation l) {
        long id = insert("""
                INSERT INTO locations (name, type, building, floor, description)
                VALUES (?, ?, ?, ?, ?)
                """,
                l.getName(), l.getType(), l.getBuilding(), l.getFloor(), l.getDescription());
        l.setId(id);
        return id;
    }

    @Override
    public void update(CampusLocation l) {
        update("""
                UPDATE locations SET name = ?, type = ?, building = ?, floor = ?, description = ?
                WHERE id = ?
                """,
                l.getName(), l.getType(), l.getBuilding(), l.getFloor(), l.getDescription(), l.getId());
    }

    @Override
    public void delete(long id) {
        update("DELETE FROM locations WHERE id = ?", id);
    }

    private static CampusLocation map(ResultSet rs) throws SQLException {
        CampusLocation l = new CampusLocation();
        l.setId(rs.getLong("id"));
        l.setName(rs.getString("name"));
        l.setType(LocationType.valueOf(rs.getString("type")));
        l.setBuilding(rs.getString("building"));
        l.setFloor(rs.getString("floor"));
        l.setDescription(rs.getString("description"));
        return l;
    }
}
