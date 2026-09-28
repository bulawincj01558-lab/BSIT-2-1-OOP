package edu.liceo.ugoautomate.dao.impl;

import edu.liceo.ugoautomate.dao.LocationDao;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.LocationType;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of {@link LocationDao}.
 */
public class JdbcLocationDao extends AbstractJdbcDao implements LocationDao {

    private static final String COLUMNS = "id, name, location_type, building, floor, description";

    public JdbcLocationDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<CampusLocation> findAll() {
        return query("SELECT " + COLUMNS + " FROM campus_locations ORDER BY location_type, name",
                NO_PARAMS, JdbcLocationDao::map);
    }

    @Override
    public Optional<CampusLocation> findById(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM campus_locations WHERE id = ?",
                ps -> ps.setLong(1, id), JdbcLocationDao::map);
    }

    @Override
    public long insert(CampusLocation location) {
        long id = insert("""
                INSERT INTO campus_locations (name, location_type, building, floor, description)
                VALUES (?, ?, ?, ?, ?)
                """, ps -> bindFields(ps, location));
        location.setId(id);
        return id;
    }

    @Override
    public void update(CampusLocation location) {
        update("""
                UPDATE campus_locations
                SET name = ?, location_type = ?, building = ?, floor = ?, description = ?
                WHERE id = ?
                """, ps -> {
            bindFields(ps, location);
            ps.setLong(6, location.getId());
        });
    }

    @Override
    public boolean delete(long id) {
        return update("DELETE FROM campus_locations WHERE id = ?", ps -> ps.setLong(1, id)) > 0;
    }

    private static void bindFields(PreparedStatement ps, CampusLocation l) throws SQLException {
        ps.setString(1, l.getName());
        ps.setString(2, l.getType().name());
        ps.setString(3, l.getBuilding());
        ps.setString(4, l.getFloor());
        ps.setString(5, l.getDescription());
    }

    private static CampusLocation map(ResultSet rs) throws SQLException {
        CampusLocation l = new CampusLocation();
        l.setId(rs.getLong("id"));
        l.setName(rs.getString("name"));
        l.setType(LocationType.valueOf(rs.getString("location_type")));
        l.setBuilding(rs.getString("building"));
        l.setFloor(rs.getString("floor"));
        l.setDescription(rs.getString("description"));
        return l;
    }
}
