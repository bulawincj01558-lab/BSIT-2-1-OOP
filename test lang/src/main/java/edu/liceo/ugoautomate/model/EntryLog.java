package edu.liceo.ugoautomate.model;

import java.time.LocalDateTime;

/**
 * A recorded, approved campus entry. Name and identifier are stored as a
 * snapshot so the log remains meaningful if the account is later removed.
 */
public class EntryLog {

    private long id;
    private EntrantType entrantType;
    private Long userId;
    private Long visitId;
    private String entrantName;
    private String identifier;
    private String details;
    private VerificationMethod method;
    private Long verifiedBy;
    private String verifiedByName;
    private LocalDateTime entryTime;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public EntrantType getEntrantType() {
        return entrantType;
    }

    public void setEntrantType(EntrantType entrantType) {
        this.entrantType = entrantType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVisitId() {
        return visitId;
    }

    public void setVisitId(Long visitId) {
        this.visitId = visitId;
    }

    public String getEntrantName() {
        return entrantName;
    }

    public void setEntrantName(String entrantName) {
        this.entrantName = entrantName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public VerificationMethod getMethod() {
        return method;
    }

    public void setMethod(VerificationMethod method) {
        this.method = method;
    }

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Long verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public String getVerifiedByName() {
        return verifiedByName;
    }

    public void setVerifiedByName(String verifiedByName) {
        this.verifiedByName = verifiedByName;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }
}
