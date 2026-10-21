package edu.liceo.ugoautomate;

import edu.liceo.ugoautomate.dao.AttendanceRecordDao;
import edu.liceo.ugoautomate.dao.AttendanceSessionDao;
import edu.liceo.ugoautomate.dao.ClassOfferingDao;
import edu.liceo.ugoautomate.dao.EntryLogDao;
import edu.liceo.ugoautomate.dao.FacultyDao;
import edu.liceo.ugoautomate.dao.GuestVisitDao;
import edu.liceo.ugoautomate.dao.LocationDao;
import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.dao.impl.JdbcAttendanceRecordDao;
import edu.liceo.ugoautomate.dao.impl.JdbcAttendanceSessionDao;
import edu.liceo.ugoautomate.dao.impl.JdbcClassOfferingDao;
import edu.liceo.ugoautomate.dao.impl.JdbcDisciplineDao;
import edu.liceo.ugoautomate.dao.impl.JdbcTuitionDao;
import edu.liceo.ugoautomate.dao.impl.JdbcEntryLogDao;
import edu.liceo.ugoautomate.dao.impl.JdbcFacultyDao;
import edu.liceo.ugoautomate.dao.impl.JdbcFacultyEvaluationDao;
import edu.liceo.ugoautomate.dao.impl.JdbcGuestVisitDao;
import edu.liceo.ugoautomate.dao.impl.JdbcLocationDao;
import edu.liceo.ugoautomate.dao.impl.JdbcSettingsDao;
import edu.liceo.ugoautomate.dao.impl.JdbcUserDao;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.PasswordHasher;
import edu.liceo.ugoautomate.security.QrTokenService;
import edu.liceo.ugoautomate.security.SecretKeyProvider;
import edu.liceo.ugoautomate.security.SessionManager;
import edu.liceo.ugoautomate.service.AcademicTerm;
import edu.liceo.ugoautomate.service.AccountService;
import edu.liceo.ugoautomate.service.AttendanceService;
import edu.liceo.ugoautomate.service.AuthService;
import edu.liceo.ugoautomate.service.ClassProgramService;
import edu.liceo.ugoautomate.service.DisciplineService;
import edu.liceo.ugoautomate.service.EntryService;
import edu.liceo.ugoautomate.service.EvaluationService;
import edu.liceo.ugoautomate.service.FacultyService;
import edu.liceo.ugoautomate.service.GuestVisitService;
import edu.liceo.ugoautomate.service.LocationService;
import edu.liceo.ugoautomate.service.ProfileService;
import edu.liceo.ugoautomate.service.TuitionService;
import edu.liceo.ugoautomate.util.Database;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Clock;

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
    private final FacultyService facultyService;
    private final EvaluationService evaluationService;
    private final ClassProgramService classProgramService;
    private final DisciplineService disciplineService;
    private final TuitionService tuitionService;

    private AppContext(Database database, Clock clock) {
        this.database = database;
        DataSource ds = database.getDataSource();

        UserDao userDao = new JdbcUserDao(ds);
        LocationDao locationDao = new JdbcLocationDao(ds);
        AttendanceSessionDao sessionDao = new JdbcAttendanceSessionDao(ds);
        AttendanceRecordDao recordDao = new JdbcAttendanceRecordDao(ds);
        GuestVisitDao visitDao = new JdbcGuestVisitDao(ds);
        EntryLogDao entryLogDao = new JdbcEntryLogDao(ds);
        FacultyDao facultyDao = new JdbcFacultyDao(ds);
        JdbcSettingsDao settingsDao = new JdbcSettingsDao(ds);
        ClassOfferingDao classDao = new JdbcClassOfferingDao(ds);

        PasswordHasher hasher = new PasswordHasher();
        this.session = new SessionManager();
        AccessControl access = new AccessControl(session);
        QrTokenService qrTokens = new QrTokenService(SecretKeyProvider.loadOrCreate(settingsDao), clock);

        this.authService = new AuthService(userDao, hasher, session, clock);
        this.profileService = new ProfileService(userDao, hasher, session, access);
        this.accountService = new AccountService(userDao, hasher, access, authService);
        this.locationService = new LocationService(locationDao, access);
        this.guestVisitService = new GuestVisitService(visitDao, qrTokens, access, clock);
        this.entryService = new EntryService(userDao, visitDao, entryLogDao, qrTokens, access, clock);
        this.facultyService = new FacultyService(facultyDao, access);
        AcademicTerm term = new AcademicTerm(settingsDao, clock);
        this.attendanceService = new AttendanceService(sessionDao, recordDao, userDao, classDao, term,
                qrTokens, access, clock);
        this.evaluationService = new EvaluationService(facultyDao, new JdbcFacultyEvaluationDao(ds), classDao,
                settingsDao, term, access, clock);
        this.classProgramService = new ClassProgramService(classDao, term, access);
        this.disciplineService = new DisciplineService(new JdbcDisciplineDao(ds), userDao, access, clock);
        this.tuitionService = new TuitionService(new JdbcTuitionDao(ds), userDao, settingsDao, term, access, clock);
    }

    public static AppContext create(Path dbFile) {
        return create(dbFile, Clock.systemDefaultZone());
    }

    public static AppContext create(Path dbFile, Clock clock) {
        return create(Database.open(dbFile), clock);
    }

    public static AppContext create(Database database, Clock clock) {
        try {
            return new AppContext(database, clock);
        } catch (RuntimeException e) {
            database.close();
            throw e;
        }
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

    public GuestVisitService guestVisits() {
        return guestVisitService;
    }

    public EntryService entries() {
        return entryService;
    }

    public FacultyService faculty() {
        return facultyService;
    }

    public EvaluationService evaluations() {
        return evaluationService;
    }

    public ClassProgramService classPrograms() {
        return classProgramService;
    }

    public DisciplineService discipline() {
        return disciplineService;
    }

    public TuitionService tuition() {
        return tuitionService;
    }

    @Override
    public void close() {
        database.close();
    }
}
