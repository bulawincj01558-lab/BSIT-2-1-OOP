package edu.liceo.ugoautomate.security;

import java.time.Instant;

public final class QrToken {
    private final QrTokenType type;
    private final long subjectId;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final String encoded;

    QrToken(QrTokenType type, long subjectId, Instant issuedAt, Instant expiresAt, String encoded) {
        this.type = type;
        this.subjectId = subjectId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.encoded = encoded;
    }

    public QrTokenType getType() {
        return type;
    }

    public long getSubjectId() {
        return subjectId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getEncoded() {
        return encoded;
    }

    @Override
    public String toString() {
        return type + "#" + subjectId + " (expires " + expiresAt + ")";
    }
}
