package edu.liceo.ugoautomate.service;

/**
 * Requested profile changes. Fields left {@code null} are not changed.
 * {@link ProfileService} applies only the fields the user's role may edit.
 */
public class ProfileUpdate {

    private String fullName;
    private String email;
    private String contactNumber;
    private String course;
    private Integer yearLevel;
    private String purposeOfVisit;
    private String personToVisit;
    private String office;

    public String getFullName() {
        return fullName;
    }

    public ProfileUpdate setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public ProfileUpdate setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public ProfileUpdate setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
        return this;
    }

    public String getCourse() {
        return course;
    }

    public ProfileUpdate setCourse(String course) {
        this.course = course;
        return this;
    }

    public Integer getYearLevel() {
        return yearLevel;
    }

    public ProfileUpdate setYearLevel(Integer yearLevel) {
        this.yearLevel = yearLevel;
        return this;
    }

    public String getPurposeOfVisit() {
        return purposeOfVisit;
    }

    public ProfileUpdate setPurposeOfVisit(String purposeOfVisit) {
        this.purposeOfVisit = purposeOfVisit;
        return this;
    }

    public String getPersonToVisit() {
        return personToVisit;
    }

    public ProfileUpdate setPersonToVisit(String personToVisit) {
        this.personToVisit = personToVisit;
        return this;
    }

    public String getOffice() {
        return office;
    }

    public ProfileUpdate setOffice(String office) {
        this.office = office;
        return this;
    }
}
