package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.SettingsDao;
import edu.liceo.ugoautomate.dao.TuitionDao;
import edu.liceo.ugoautomate.dao.UserDao;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.Student;
import edu.liceo.ugoautomate.model.TuitionAccount;
import edu.liceo.ugoautomate.model.TuitionEntry;
import edu.liceo.ugoautomate.model.TuitionEntryType;
import edu.liceo.ugoautomate.model.TuitionStatement;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.util.Money;
import edu.liceo.ugoautomate.util.Validation;
import edu.liceo.ugoautomate.util.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TuitionService {
    static final String TUITION_PREFIX = "Tuition fee";
    static final String MISC_DESCRIPTION = "Miscellaneous fees";

    private static final String RATE_KEY = "tuition.rate_per_unit_cents";
    private static final String MISC_KEY = "tuition.misc_fee_cents";
    private static final long DEFAULT_RATE_CENTS = 120_000;
    private static final long DEFAULT_MISC_CENTS = 500_000;

    private static final long MAX_AMOUNT_CENTS = 1_000_000_000L;

    public record Rates(long perUnitCents, long miscFeeCents) {
    }

    public record AssessmentResult(int assessed, int alreadyAssessed) {
    }

    private final TuitionDao tuitionDao;
    private final UserDao userDao;
    private final SettingsDao settings;
    private final AcademicTerm term;
    private final AccessControl access;
    private final Clock clock;

    public TuitionService(TuitionDao tuitionDao, UserDao userDao, SettingsDao settings, AcademicTerm term,
                          AccessControl access, Clock clock) {
        this.tuitionDao = tuitionDao;
        this.userDao = userDao;
        this.settings = settings;
        this.term = term;
        this.access = access;
        this.clock = clock;
    }

    public TuitionStatement myStatement() {
        User student = access.requireRole(Role.STUDENT);
        String current = term.current();
        return new TuitionStatement(current, tuitionDao.findEntries(student.getId(), current));
    }

    public List<String> terms() {
        access.requireRole(Role.ADMIN);
        String current = term.current();
        List<String> terms = new ArrayList<>(List.of(current));
        tuitionDao.findTerms().stream().filter(t -> !t.equals(current)).forEach(terms::add);
        return terms;
    }

    public List<TuitionAccount> accounts(String forTerm, String search) {
        access.requireRole(Role.ADMIN);
        return tuitionDao.findAccounts(forTerm, search);
    }

    public TuitionStatement statement(long studentId, String forTerm) {
        access.requireRole(Role.ADMIN);
        return new TuitionStatement(forTerm, tuitionDao.findEntries(studentId, forTerm));
    }

    public TuitionEntry addEntry(long studentId, String forTerm, TuitionEntryType type, String description,
                                 long amountCents, String reference, LocalDate date) {
        User admin = access.requireRole(Role.ADMIN);
        if (!(userDao.findById(studentId).orElse(null) instanceof Student)) {
            throw new ServiceException("Student not found.");
        }
        if (type == null) {
            throw new ValidationException("Choose the kind of entry.");
        }
        if (amountCents <= 0 || amountCents > MAX_AMOUNT_CENTS) {
            throw new ValidationException("The amount must be between ₱0.01 and " + Money.format(MAX_AMOUNT_CENTS) + ".");
        }
        if (date == null) {
            throw new ValidationException("Choose the date.");
        }
        if (date.isAfter(LocalDate.now(clock))) {
            throw new ValidationException("The date cannot be in the future.");
        }
        TuitionEntry entry = new TuitionEntry();
        entry.setStudentId(studentId);
        entry.setTerm(Validation.required(forTerm, "Term", 60));
        entry.setType(type);
        entry.setDescription(Validation.required(description, "Description", 120));
        entry.setAmountCents(amountCents);
        entry.setReference(Validation.optional(reference, 40, "Reference / OR number"));
        entry.setEntryDate(date);
        entry.setRecordedBy(admin.getId());
        entry.setRecordedAt(LocalDateTime.now(clock));
        tuitionDao.create(entry);
        return entry;
    }

    public void deleteEntry(long entryId) {
        access.requireRole(Role.ADMIN);
        tuitionDao.findById(entryId).orElseThrow(() -> new ServiceException("Ledger entry not found."));
        tuitionDao.delete(entryId);
    }

    public Rates rates() {
        access.requireRole(Role.ADMIN);
        return new Rates(settings.get(RATE_KEY).map(Long::parseLong).orElse(DEFAULT_RATE_CENTS),
                settings.get(MISC_KEY).map(Long::parseLong).orElse(DEFAULT_MISC_CENTS));
    }

    public AssessmentResult assess(String forTerm, Rates rates) {
        User admin = access.requireRole(Role.ADMIN);
        String cleanedTerm = Validation.required(forTerm, "Term", 60);
        if (rates.perUnitCents() <= 0 || rates.perUnitCents() > MAX_AMOUNT_CENTS
                || rates.miscFeeCents() < 0 || rates.miscFeeCents() > MAX_AMOUNT_CENTS) {
            throw new ValidationException("Enter a rate per unit above zero and a miscellaneous fee of zero or more.");
        }
        settings.put(RATE_KEY, Long.toString(rates.perUnitCents()));
        settings.put(MISC_KEY, Long.toString(rates.miscFeeCents()));

        int assessed = 0;
        int already = 0;
        LocalDate today = LocalDate.now(clock);
        for (Map.Entry<Long, Integer> student : tuitionDao.findEnrolledUnits(cleanedTerm).entrySet()) {
            long studentId = student.getKey();
            int units = student.getValue();
            if (units <= 0) {
                continue;
            }
            if (tuitionDao.hasCharge(studentId, cleanedTerm, TUITION_PREFIX)) {
                already++;
                continue;
            }
            charge(admin, studentId, cleanedTerm, TUITION_PREFIX + " (" + units + " units x "
                    + Money.format(rates.perUnitCents()) + ")", units * rates.perUnitCents(), today);
            if (rates.miscFeeCents() > 0) {
                charge(admin, studentId, cleanedTerm, MISC_DESCRIPTION, rates.miscFeeCents(), today);
            }
            assessed++;
        }
        return new AssessmentResult(assessed, already);
    }

    private void charge(User admin, long studentId, String forTerm, String description, long cents, LocalDate date) {
        TuitionEntry entry = new TuitionEntry();
        entry.setStudentId(studentId);
        entry.setTerm(forTerm);
        entry.setType(TuitionEntryType.CHARGE);
        entry.setDescription(description);
        entry.setAmountCents(cents);
        entry.setEntryDate(date);
        entry.setRecordedBy(admin.getId());
        entry.setRecordedAt(LocalDateTime.now(clock));
        tuitionDao.create(entry);
    }
}
