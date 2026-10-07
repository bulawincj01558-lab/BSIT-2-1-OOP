package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.SettingsDao;

import java.time.Clock;
import java.time.LocalDate;

public class AcademicTerm {
    static final String TERM_KEY = "evaluation.term";

    private final SettingsDao settings;
    private final Clock clock;

    public AcademicTerm(SettingsDao settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    public String current() {
        return settings.get(TERM_KEY).orElseGet(() -> defaultTerm(LocalDate.now(clock)));
    }

    void set(String term) {
        settings.put(TERM_KEY, term);
    }

    static String defaultTerm(LocalDate date) {
        int year = date.getYear();
        int month = date.getMonthValue();
        if (month >= 6 && month <= 10) {
            return "SY " + year + "-" + (year + 1) + " 1st Semester";
        }
        if (month >= 11) {
            return "SY " + year + "-" + (year + 1) + " 2nd Semester";
        }
        if (month <= 3) {
            return "SY " + (year - 1) + "-" + year + " 2nd Semester";
        }
        return "SY " + (year - 1) + "-" + year + " Summer";
    }
}
