package edu.liceo.ugoautomate;

import edu.liceo.ugoautomate.dao.AttendanceRecordDao;
import edu.liceo.ugoautomate.dao.AttendanceSessionDao;
import edu.liceo.ugoautomate.dao.EntryLogDao;
import edu.liceo.ugoautomate.dao.GuestVisitDao;
import edu.liceo.ugoautomate.dao.LocationDao;
import edu.liceo.ugoautomate.dao.SettingsDao;
import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.dao.impl.JdbcAttendanceRecordDao;
import edu.liceo.ugoautomate.dao.impl.JdbcAttendanceSessionDao;
import edu.liceo.ugoautomate.dao.impl.JdbcEntryLogDao;
import edu.liceo.ugoautomate.dao.impl.JdbcGuestVisitDao;
import edu.liceo.ugoautomate.dao.impl.JdbcLocationDao;
import edu.liceo.ugoautomate.dao.impl.JdbcSettingsDao;
import edu.liceo.ugoautomate.dao.impl.JdbcUserDao;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.QrTokenService;
import edu.liceo.ugoautomate.security.SecretKeyProvider;
import edu.liceo.ugoautomate.security.SessionManager;
import edu.liceo.ugoautomate.service.AccountService;
import edu.liceo.ugoautomate.service.AttendanceService;
import edu.liceo.ugoautomate.service.AuthService;
import edu.liceo.ugoautomate.service.EntryService;
import edu.liceo.ugoautomate.service.GuestVisitService;
import edu.liceo.ugoautomate.service.LocationService;
import edu.liceo.ugoautomate.service.ProfileService;
import edu.liceo.ugoautomate.util.Database;
import edu.liceo.ugoautomate.util.DatabaseInitializer;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Composition root: builds the database pool, DAOs, security components, and
 * services once, and hands the services to the UI. The UI never touches DAOs
 * directly.
 */
public final class AppContext implements AutoCloseable {

    private final Database database;
    private final SessionManager session;
    private final AuthService authService;
    private final ProfileService profileService;
    private final AccountService accountService;
    private final LocationService locationService;
    private final AttendanceService attendanceService;
    private final GuestVisitService guestVisitService;
    private final EntryService entryService;

    private AppContext(Path databaseFile, Clock clock) {
        this.database = new Database(databaseFile);
        DataSource ds = database.getDataSource();

        UserDao userDao = new JdbcUserDao(ds);
        LocationDao locationDao = new JdbcLocationDao(ds);
        AttendanceSessionDao sessionDao = new JdbcAttendanceSessionDao(ds);
        AttendanceRecordDao recordDao = new JdbcAttendanceRecordDao(ds);
        GuestVisitDao visitDao = new JdbcGuestVisitDao(ds);
        EntryLogDao entryLogDao = new JdbcEntryLogDao(ds);
        SettingsDao settingsDao = new JdbcSettingsDao(ds);

        new DatabaseInitializer(ds, userDao, clock).initialize();

        this.session = new SessionManager();
        AccessControl access = new AccessControl(session, userDao);
        QrTokenService qrTokens = new QrTokenService(SecretKeyProvider.loadOrCreateQrSecret(settingsDao), clock);

        this.authService = new AuthService(userDao, session, access, clock);
        this.profileService = new ProfileService(userDao, access, session);
        this.accountService = new AccountService(userDao, access);
        this.locationService = new LocationService(locationDao, access, clock);
        this.attendanceService = new AttendanceService(sessionDao, recordDao, userDao, access, qrTokens, clock);
        this.guestVisitService = new GuestVisitService(visitDao, access, qrTokens, clock);
        this.entryService = new EntryService(userDao, visitDao, entryLogDao, access, qrTokens, clock);
    }

    public static AppContext create(Path databaseFile) {
        return create(databaseFile, Clock.systemDefaultZone());
    }

    public static AppContext create(Path databaseFile, Clock clock) {
        return new AppContext(databaseFile, clock);
    }

    public SessionManager session() {
        return session;
    }

    public AuthService auth() {
        return authService;
    }

    public ProfileService profiles() {
        return profileService;
    }

    public AccountService accounts() {
        return accountService;
    }

    public LocationService locations() {
        return locationService;
    }

    public AttendanceService attendance() {
        return attendanceService;
    }

    public GuestVisitService visits() {
        return guestVisitService;
    }

    public EntryService entries() {
        return entryService;
    }

    @Override
    public void close() {
        database.close();
    }
}
