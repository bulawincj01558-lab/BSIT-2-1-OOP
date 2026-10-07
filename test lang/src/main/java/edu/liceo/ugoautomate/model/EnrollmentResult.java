package edu.liceo.ugoautomate.model;

import java.util.List;

public record EnrollmentResult(int enrolled, int alreadyEnrolled, List<String> conflicts) {
}
