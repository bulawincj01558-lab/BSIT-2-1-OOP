package edu.liceo.ugoautomate.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A guest's registered visit for a specific day. The {@code passNonce} binds
 * the visit pass QR code to this record so forged or reused passes fail.
 */
public class GuestVisit {

    private long id;
    private long guestUserId;
    private String guestName;
    private String guestContact;
    private String purpose;
    private String personToVisit;
    private LocalDate visitDate;
    private VisitStatus status;
    private String passNonce;
    private LocalDateTime registeredAt;
    private LocalDateTime checkedInAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGuestUserId() {
        return guestUserId;
    }

    public void setGuestUserId(long guestUserId) {
        this.guestUserId = guestUserId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestContact() {
        return guestContact;
    }

    public void setGuestContact(String guestContact) {
        this.guestContact = guestContact;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getPersonToVisit() {
        return personToVisit;
    }

    public void setPersonToVisit(String personToVisit) {
        this.personToVisit = personToVisit;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public VisitStatus getStatus() {
        return status;
    }

    public void setStatus(VisitStatus status) {
        this.status = status;
    }

    public String getPassNonce() {
        return passNonce;
    }

    public void setPassNonce(String passNonce) {
        this.passNonce = passNonce;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDateTime getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(LocalDateTime checkedInAt) {
        this.checkedInAt = checkedInAt;
    }
}
