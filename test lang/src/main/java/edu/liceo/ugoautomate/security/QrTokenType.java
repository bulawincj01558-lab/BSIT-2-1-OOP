package edu.liceo.ugoautomate.security;

import java.util.Optional;

/**
 * Purposes a signed QR code can be issued for. The short code is embedded in
 * the payload so a code issued for one purpose can never be used for another.
 */
public enum QrTokenType {
    /** Attendance session code displayed by an administrator; subject = session id. */
    ATTENDANCE("ATT"),
    /** Student campus entry pass; subject = student user id. */
    ENTRY_PASS("ENT"),
    /** Guest visit pass; subject = guest visit id. */
    VISIT_PASS("VIS");

    private final String code;

    QrTokenType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<QrTokenType> fromCode(String code) {
        for (QrTokenType t : values()) {
            if (t.code.equals(code)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }
}
