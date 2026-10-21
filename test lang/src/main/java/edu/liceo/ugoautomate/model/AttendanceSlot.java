package edu.liceo.ugoautomate.model;

public enum AttendanceSlot {
    MORNING_IN("Morning time in", null),
    MORNING_OUT("Morning time out", MORNING_IN),
    AFTERNOON_IN("Afternoon time in", null),
    AFTERNOON_OUT("Afternoon time out", AFTERNOON_IN);

    private final String label;
    private final AttendanceSlot requires;

    AttendanceSlot(String label, AttendanceSlot requires) {
        this.label = label;
        this.requires = requires;
    }

    public String getLabel() {
        return label;
    }

    public AttendanceSlot getRequires() {
        return requires;
    }

    @Override
    public String toString() {
        return label;
    }
}
