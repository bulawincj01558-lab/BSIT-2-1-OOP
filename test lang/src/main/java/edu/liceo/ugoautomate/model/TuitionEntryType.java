package edu.liceo.ugoautomate.model;

public enum TuitionEntryType {
    CHARGE("Charge", 1),
    PAYMENT("Payment", -1),
    DISCOUNT("Discount / scholarship", -1);

    private final String label;
    private final int sign;

    TuitionEntryType(String label, int sign) {
        this.label = label;
        this.sign = sign;
    }

    public String getLabel() {
        return label;
    }

    public int getSign() {
        return sign;
    }

    @Override
    public String toString() {
        return label;
    }
}
