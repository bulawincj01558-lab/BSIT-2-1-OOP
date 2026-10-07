package edu.liceo.ugoautomate.model;

public class Guest extends User {
    private String purposeOfVisit;
    private String personToVisit;

    public Guest() {
        super(Role.GUEST);
    }

    @Override
    public String getSummary() {
        return "Visiting " + personToVisit + " - " + purposeOfVisit;
    }

    public String getPurposeOfVisit() {
        return purposeOfVisit;
    }

    public void setPurposeOfVisit(String purposeOfVisit) {
        this.purposeOfVisit = purposeOfVisit;
    }

    public String getPersonToVisit() {
        return personToVisit;
    }

    public void setPersonToVisit(String personToVisit) {
        this.personToVisit = personToVisit;
    }
}
