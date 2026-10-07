package edu.liceo.ugoautomate.model;

public class Administrator extends User {
    private String position;

    public Administrator() {
        super(Role.ADMIN);
    }

    @Override
    public String getSummary() {
        return position == null || position.isBlank() ? "Administrator" : position;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
