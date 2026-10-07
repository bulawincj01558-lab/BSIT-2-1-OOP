package edu.liceo.ugoautomate.model;

public record TuitionAccount(long studentId, String studentNumber, String name, String course, int yearLevel,
                             long chargesCents, long paymentsCents, long discountsCents) {
    public long balanceCents() {
        return chargesCents - paymentsCents - discountsCents;
    }
}
