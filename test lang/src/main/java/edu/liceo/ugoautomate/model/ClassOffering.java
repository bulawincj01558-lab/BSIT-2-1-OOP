package edu.liceo.ugoautomate.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassOffering {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private long id;
    private String term;
    private String subjectCode;
    private String subjectTitle;
    private int units;
    private String section;
    private Long facultyId;
    private String facultyName;
    private final Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private int enrolledCount;

    public String getDaysText() {
        return days.stream().map(ClassOffering::dayCode).collect(Collectors.joining());
    }

    public String getScheduleText() {
        if (startTime == null || endTime == null) {
            return getDaysText();
        }
        return getDaysText() + " " + TIME.format(startTime) + " - " + TIME.format(endTime);
    }

    public boolean conflictsWith(ClassOffering other) {
        boolean sharedDay = days.stream().anyMatch(other.days::contains);
        return sharedDay && startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    public static String dayCode(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "M";
            case TUESDAY -> "T";
            case WEDNESDAY -> "W";
            case THURSDAY -> "Th";
            case FRIDAY -> "F";
            case SATURDAY -> "S";
            case SUNDAY -> "Su";
        };
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectTitle() {
        return subjectTitle;
    }

    public void setSubjectTitle(String subjectTitle) {
        this.subjectTitle = subjectTitle;
    }

    public int getUnits() {
        return units;
    }

    public void setUnits(int units) {
        this.units = units;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    public String getFacultyName() {
        return facultyName == null ? "TBA" : facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public Set<DayOfWeek> getDays() {
        return days.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(days);
    }

    public void setDays(Set<DayOfWeek> newDays) {
        days.clear();
        days.addAll(newDays);
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public int getEnrolledCount() {
        return enrolledCount;
    }

    public void setEnrolledCount(int enrolledCount) {
        this.enrolledCount = enrolledCount;
    }

    @Override
    public String toString() {
        return subjectCode + " " + section;
    }
}
