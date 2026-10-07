package edu.liceo.ugoautomate.model;

import java.util.List;

public record TuitionStatement(String term, List<TuitionEntry> entries) {
    public long totalCents(TuitionEntryType type) {
        return entries.stream().filter(e -> e.getType() == type).mapToLong(TuitionEntry::getAmountCents).sum();
    }

    public long balanceCents() {
        return entries.stream().mapToLong(TuitionEntry::getSignedCents).sum();
    }
}
