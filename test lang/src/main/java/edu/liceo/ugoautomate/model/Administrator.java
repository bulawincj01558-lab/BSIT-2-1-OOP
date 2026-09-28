package edu.liceo.ugoautomate.model;

/**
 * A staff account with access to management and entry verification features.
 */
public class Administrator extends User {

    private String office;

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }
}
