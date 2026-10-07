package edu.liceo.ugoautomate;

import edu.liceo.ugoautomate.model.AttendanceSession;
import edu.liceo.ugoautomate.model.CampusLocation;
import edu.liceo.ugoautomate.model.CaseStatus;
import edu.liceo.ugoautomate.model.DisciplineRecord;
import edu.liceo.ugoautomate.model.ClassOffering;
import edu.liceo.ugoautomate.model.EnrollmentResult;
import edu.liceo.ugoautomate.model.EvaluationCriterion;
import edu.liceo.ugoautomate.model.EvaluationSummary;
import edu.liceo.ugoautomate.model.Faculty;
import edu.liceo.ugoautomate.model.FacultyEvaluation;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.model.LocationType;
import edu.liceo.ugoautomate.model.OffenseSeverity;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.ScanResult;
import edu.liceo.ugoautomate.model.SessionType;
import edu.liceo.ugoautomate.model.TuitionEntryType;
import edu.liceo.ugoautomate.model.TuitionStatement;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.model.VerificationMethod;
import edu.liceo.ugoautomate.model.VisitStatus;
import edu.liceo.ugoautomate.security.AccessDeniedException;
import edu.liceo.ugoautomate.service.GuestRegistration;
import edu.liceo.ugoautomate.service.ProfileUpdate;
import edu.liceo.ugoautomate.service.ServiceException;
import edu.liceo.ugoautomate.service.StudentRegistration;
import edu.liceo.ugoautomate.service.TuitionService;
import edu.liceo.ugoautomate.util.Money;
import edu.liceo.ugoautomate.util.QrCodes;
import edu.liceo.ugoautomate.util.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampusFlowTest {
    @TempDir
    Path tempDir;

    private AppContext ctx;

    @BeforeEach
    void setUp() {
        ctx = AppContext.create(tempDir.resolve("test.db"));
    }

    @AfterEach
    void tearDown() {
        ctx.close();
    }

    @Test
    void seededAdminCanLogInAndWrongPasswordIsRejected() {
        User admin = ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        assertEquals(Role.ADMIN, admin.getRole());
        ctx.auth().logout();

        ServiceException e = assertThrows(ServiceException.class, () -> ctx.auth().login("admin@liceo.edu.ph", "wrong-pass1"));
        assertEquals("Invalid email or password.", e.getMessage());
        assertThrows(ServiceException.class, () -> ctx.auth().login("nobody@liceo.edu.ph", "Admin@123"));
    }

    @Test
    void studentRegistrationRejectsDuplicatesAndAllowsLogin() {
        ctx.auth().registerStudent(new StudentRegistration("2025-00001", "Ana", "Cruz", "BS Information Technology", 1,
                "ana.cruz@liceo.edu.ph", "0917-555-5555", "Secret123", "Secret123"));

        assertThrows(ServiceException.class, () -> ctx.auth().registerStudent(new StudentRegistration("2025-00001",
                "Other", "Person", "BSCS", 1, "other@liceo.edu.ph", null, "Secret123", "Secret123")));
        assertThrows(ServiceException.class, () -> ctx.auth().registerStudent(new StudentRegistration("2025-00002",
                "Other", "Person", "BSCS", 1, "ANA.CRUZ@liceo.edu.ph", null, "Secret123", "Secret123")));

        assertEquals(Role.STUDENT, ctx.auth().login("ana.cruz@liceo.edu.ph", "Secret123").getRole());
    }

    @Test
    void attendanceQrFlowRecordsOnceAndRejectsTampering() {
        ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        AttendanceSession session = new AttendanceSession();
        session.setTitle("IT 212 Data Structures");
        session.setType(SessionType.CLASS);
        session.setStartsAt(LocalDateTime.now().minusMinutes(10));
        session.setEndsAt(LocalDateTime.now().plusHours(1));
        ctx.attendance().createSession(session);
        String qr = ctx.attendance().issueSessionQr(session.getId()).getEncoded();
        ctx.auth().logout();

        assertEquals(qr, QrCodes.decode(QrCodes.generate(qr, 300)).orElseThrow());

        ctx.auth().login("juan.delacruz@liceo.edu.ph", "Student@123");
        ScanResult first = ctx.attendance().recordAttendance(qr, VerificationMethod.MANUAL_ENTRY);
        assertTrue(first.isSuccess(), first.toString());
        ScanResult second = ctx.attendance().recordAttendance(qr, VerificationMethod.MANUAL_ENTRY);
        assertFalse(second.isSuccess());
        assertEquals("Already recorded", second.getTitle());

        String tampered = qr.replace("." + session.getId() + ".", "." + (session.getId() + 1) + ".");
        assertFalse(ctx.attendance().recordAttendance(tampered, VerificationMethod.MANUAL_ENTRY).isSuccess());
        assertEquals(1, ctx.attendance().myAttendance().size());
        ctx.auth().logout();

        ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        assertEquals(1, ctx.attendance().listRecords(session.getId()).size());
    }

    @Test
    void studentEntryQrIsVerifiedAndLogged() {
        ctx.auth().login("maria.santos@liceo.edu.ph", "Student@123");
        String entryQr = ctx.entries().issueStudentEntryQr().getEncoded();
        ctx.auth().logout();

        ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        ScanResult result = ctx.entries().verifyEntry(entryQr, VerificationMethod.IMAGE_FILE);
        assertTrue(result.isSuccess(), result.toString());
        assertEquals(1, ctx.entries().listEntryLogs(LocalDate.now(), LocalDate.now()).size());

        AttendanceSession sample = ctx.attendance().listSessions().get(0);
        String attendanceQr = ctx.attendance().issueSessionQr(sample.getId()).getEncoded();
        assertFalse(ctx.entries().verifyEntry(attendanceQr, VerificationMethod.MANUAL_ENTRY).isSuccess());
    }

    @Test
    void guestRegistersVisitAndPassWorksOnce() {
        ctx.auth().registerGuest(new GuestRegistration("Pedro", "Garcia", "0918-123-4567", "pedro@example.com",
                "Deliver documents", "Registrar's Office", "Visit1234", "Visit1234"));
        ctx.auth().login("pedro@example.com", "Visit1234");
        GuestVisit visit = ctx.guestVisits().registerVisit("Deliver documents", "Registrar's Office", LocalDate.now());
        String pass = ctx.guestVisits().issueGuestPass(visit.getId()).getEncoded();
        ctx.auth().logout();

        ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        assertTrue(ctx.entries().verifyEntry(pass, VerificationMethod.WEBCAM).isSuccess());
        assertFalse(ctx.entries().verifyEntry(pass, VerificationMethod.WEBCAM).isSuccess());
        assertEquals(VisitStatus.CHECKED_IN, ctx.guestVisits().listVisits(LocalDate.now()).get(0).getStatus());
    }

    @Test
    void rolesAreEnforced() {
        assertThrows(AccessDeniedException.class, () -> ctx.locations().listAll());

        ctx.auth().login("jose.reyes@liceo.edu.ph", "Student@123");
        assertFalse(ctx.locations().search("library", null).isEmpty());
        assertThrows(AccessDeniedException.class, () -> ctx.accounts().listAccounts(null, null));
        assertThrows(AccessDeniedException.class, () -> ctx.entries().verifyEntry("x", VerificationMethod.MANUAL_ENTRY));
        CampusLocation loc = new CampusLocation();
        loc.setName("Hack");
        loc.setType(LocationType.OFFICE);
        assertThrows(AccessDeniedException.class, () -> ctx.locations().create(loc));
    }

    @Test
    void profileUpdateChangesOnlyPermittedFields() {
        ctx.auth().login("juan.delacruz@liceo.edu.ph", "Student@123");
        User me = ctx.profiles().myProfile();
        ProfileUpdate current = ProfileUpdate.of(me);
        ctx.profiles().updateMyProfile(new ProfileUpdate("Juanito", current.lastName(), current.contactNumber(),
                current.course(), 3, null, null, null));

        User reloaded = ctx.profiles().myProfile();
        assertEquals("Juanito", reloaded.getFirstName());
        assertEquals("juan.delacruz@liceo.edu.ph", reloaded.getEmail());
        assertEquals("Juanito", ctx.session().current().orElseThrow().getFirstName());
    }

    @Test
    void adminManagesLocationsAndAccounts() {
        ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        CampusLocation loc = new CampusLocation();
        loc.setName("Robotics Lab");
        loc.setType(LocationType.FACILITY);
        loc.setBuilding("Engineering Building");
        ctx.locations().create(loc);
        assertEquals(1, ctx.locations().search("robotics", LocationType.FACILITY).size());
        ctx.locations().delete(loc.getId());
        assertTrue(ctx.locations().search("robotics", null).isEmpty());

        List<User> students = ctx.accounts().listAccounts(Role.STUDENT, null);
        assertEquals(3, students.size());
        long joseId = ctx.accounts().listAccounts(null, "Reyes").get(0).getId();
        ctx.accounts().setActive(joseId, false);
        ctx.auth().logout();
        assertThrows(ServiceException.class, () -> ctx.auth().login("jose.reyes@liceo.edu.ph", "Student@123"));
    }

    @Test
    void studentEvaluatesFacultyOncePerTermAndAdminSeesAnonymousAverages() {
        ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        ctx.evaluations().setPeriod("SY 2026-2027 1st Semester", true);
        ctx.auth().logout();

        ctx.auth().login("juan.delacruz@liceo.edu.ph", "Student@123");
        Faculty teacher = ctx.evaluations().facultyToEvaluate().get(0);
        Map<EvaluationCriterion, Integer> ratings = new EnumMap<>(EvaluationCriterion.class);
        for (EvaluationCriterion c : EvaluationCriterion.values()) {
            ratings.put(c, 4);
        }
        ratings.put(EvaluationCriterion.CLARITY, 5);
        ctx.evaluations().submit(teacher.getId(), ratings, "Explains clearly.");
        assertTrue(ctx.evaluations().myEvaluatedFacultyIds().contains(teacher.getId()));
        assertThrows(ServiceException.class, () -> ctx.evaluations().submit(teacher.getId(), ratings, null));

        Map<EvaluationCriterion, Integer> incomplete = new EnumMap<>(ratings);
        incomplete.remove(EvaluationCriterion.FAIRNESS);
        long otherId = ctx.evaluations().facultyToEvaluate().get(1).getId();
        assertThrows(ValidationException.class, () -> ctx.evaluations().submit(otherId, incomplete, null));
        assertThrows(AccessDeniedException.class, () -> ctx.evaluations().summaries("SY 2026-2027 1st Semester"));
        ctx.auth().logout();

        ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        EvaluationSummary summary = ctx.evaluations().summaries("SY 2026-2027 1st Semester").stream()
                .filter(s -> s.getFaculty().getId() == teacher.getId()).findFirst().orElseThrow();
        assertEquals(1, summary.getCount());
        assertEquals(5.0, summary.getAverage(EvaluationCriterion.CLARITY));
        assertEquals(4.2, summary.getOverall(), 0.0001);
        FacultyEvaluation written = ctx.evaluations().evaluationsOf(teacher.getId(), "SY 2026-2027 1st Semester").get(0);
        assertEquals("Explains clearly.", written.getComment());
        assertEquals(0, written.getStudentId());

        ctx.evaluations().setPeriod("SY 2026-2027 1st Semester", false);
        ctx.auth().logout();
        ctx.auth().login("maria.santos@liceo.edu.ph", "Student@123");
        assertThrows(ServiceException.class, () -> ctx.evaluations().submit(teacher.getId(), ratings, null));
    }

    @Test
    void classProgramsEnrollStudentsWithoutScheduleConflicts() {
        String term = "SY 2026-2027 1st Semester";
        ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        ctx.evaluations().setPeriod(term, true);

        ClassOffering clash = newClass(term, "IT 299", Set.of(DayOfWeek.MONDAY), LocalTime.of(8, 30), LocalTime.of(9, 30));
        ctx.classPrograms().create(clash);
        assertThrows(ServiceException.class, () -> ctx.classPrograms().enrollByStudentNumber(clash.getId(), "2023-00123"));

        ClassOffering evening = newClass(term, "IT 300", Set.of(DayOfWeek.MONDAY), LocalTime.of(17, 0), LocalTime.of(18, 0));
        ctx.classPrograms().create(evening);
        EnrollmentResult result = ctx.classPrograms().enrollCourse(evening.getId(), "BS Information Technology", 2);
        assertEquals(1, result.enrolled());
        assertTrue(result.conflicts().isEmpty());
        assertEquals(1, ctx.classPrograms().roster(evening.getId()).size());

        ClassOffering bad = newClass(term, "IT 301", Set.of(DayOfWeek.TUESDAY), LocalTime.of(10, 0), LocalTime.of(9, 0));
        assertThrows(ValidationException.class, () -> ctx.classPrograms().create(bad));
        ctx.auth().logout();

        ctx.auth().login("juan.delacruz@liceo.edu.ph", "Student@123");
        List<ClassOffering> program = ctx.classPrograms().myClassProgram();
        assertEquals(6, program.size());
        assertTrue(program.stream().anyMatch(c -> c.getSubjectCode().equals("IT 300")));
        assertEquals(5, ctx.evaluations().facultyToEvaluate().size());
        assertThrows(AccessDeniedException.class, () -> ctx.classPrograms().listClasses(term));
    }

    @Test
    void adminRecordsDisciplineCasesAndStudentsSeeOnlyTheirOwn() {
        ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        assertEquals(1, ctx.discipline().openCaseCount());
        DisciplineRecord record = new DisciplineRecord();
        record.setIncidentDate(LocalDate.now().minusDays(1));
        record.setOffense("Cutting classes");
        record.setSeverity(OffenseSeverity.MAJOR);
        record.setSanction("Community service");
        ctx.discipline().create("2023-00123", record);
        assertEquals(2, ctx.discipline().openCaseCount());
        assertThrows(ServiceException.class, () -> ctx.discipline().create("2099-99999", record));

        DisciplineRecord future = new DisciplineRecord();
        future.setIncidentDate(LocalDate.now().plusDays(3));
        future.setOffense("Littering");
        future.setSeverity(OffenseSeverity.MINOR);
        assertThrows(ValidationException.class, () -> ctx.discipline().create("2023-00123", future));

        record.setStatus(CaseStatus.RESOLVED);
        record.setResolution("Completed 10 hours of service.");
        ctx.discipline().update(record);
        assertEquals(1, ctx.discipline().openCaseCount());
        assertEquals(1, ctx.discipline().search(CaseStatus.RESOLVED, "juan").size());
        ctx.auth().logout();

        ctx.auth().login("juan.delacruz@liceo.edu.ph", "Student@123");
        List<DisciplineRecord> mine = ctx.discipline().myRecord();
        assertEquals(1, mine.size());
        assertEquals(CaseStatus.RESOLVED, mine.get(0).getStatus());
        assertTrue(mine.get(0).getResolvedAt() != null);
        assertThrows(AccessDeniedException.class, () -> ctx.discipline().search(null, null));
    }

    @Test
    void tuitionLedgerBalancesAndAssessmentChargesEnrolledUnitsOnce() {
        String term = "SY 2026-2027 1st Semester";
        ctx.auth().login("admin@liceo.edu.ph", "Admin@123");
        ctx.evaluations().setPeriod(term, true);
        long juanId = ctx.accounts().listAccounts(null, "Dela Cruz").get(0).getId();
        assertEquals(1_300_000, balanceOf(juanId, term));

        ctx.tuition().addEntry(juanId, term, TuitionEntryType.DISCOUNT, "Academic scholarship", 300_000, null, LocalDate.now());
        ctx.tuition().addEntry(juanId, term, TuitionEntryType.PAYMENT, "Payment - Cash", Money.parse("2,500.50", "Amount"), "OR 1", LocalDate.now());
        assertEquals(749_950, balanceOf(juanId, term));
        assertThrows(ValidationException.class, () -> ctx.tuition().addEntry(juanId, term, TuitionEntryType.PAYMENT,
                "Payment - Cash", 100, null, LocalDate.now().plusDays(1)));
        assertThrows(ValidationException.class, () -> Money.parse("12.345", "Amount"));

        ClassOffering extra = newClass(term, "IT 300", Set.of(DayOfWeek.MONDAY), LocalTime.of(17, 0), LocalTime.of(18, 0));
        ctx.classPrograms().create(extra);
        ctx.classPrograms().enrollByStudentNumber(extra.getId(), "2022-00456");
        TuitionService.AssessmentResult first = ctx.tuition().assess(term, new TuitionService.Rates(100_000, 50_000));
        assertEquals(1, first.assessed());
        assertEquals(1, first.alreadyAssessed());
        assertEquals(0, ctx.tuition().assess(term, ctx.tuition().rates()).assessed());
        long mariaId = ctx.accounts().listAccounts(null, "Santos").get(0).getId();
        assertEquals(350_000, balanceOf(mariaId, term));
        ctx.auth().logout();

        ctx.auth().login("juan.delacruz@liceo.edu.ph", "Student@123");
        TuitionStatement mine = ctx.tuition().myStatement();
        assertEquals(749_950, mine.balanceCents());
        assertEquals(5, mine.entries().size());
        assertThrows(AccessDeniedException.class, () -> ctx.tuition().accounts(term, null));
    }

    private long balanceOf(long studentId, String term) {
        return ctx.tuition().accounts(term, null).stream()
                .filter(a -> a.studentId() == studentId).findFirst().orElseThrow().balanceCents();
    }

    private static ClassOffering newClass(String term, String code, Set<DayOfWeek> days, LocalTime start, LocalTime end) {
        ClassOffering c = new ClassOffering();
        c.setTerm(term);
        c.setSubjectCode(code);
        c.setSubjectTitle("Test subject");
        c.setUnits(3);
        c.setSection("BSIT 2-A");
        c.setDays(days);
        c.setStartTime(start);
        c.setEndTime(end);
        return c;
    }
}
