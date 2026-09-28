package edu.liceo.ugoautomate.model;

/**
 * A campus visitor. The purpose of visit and person/office to visit captured
 * at registration are used as defaults when the guest registers a visit.
 */
public class Guest extends User {

    private String purposeOfVisit;
    private String personToVisit;

    @Override
    public Role getRole() {
        return Role.GUEST;
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
