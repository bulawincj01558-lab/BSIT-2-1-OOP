package edu.liceo.ugoautomate.ds;

import java.util.List;

public record StructureInfo(
        StructureKind kind,
        String title,
        String javaType,
        String usedIn,
        String allocation,
        String spaceComplexity,
        List<Complexity> operations) {
    @Override
    public String toString() {
        return title;
    }
}
