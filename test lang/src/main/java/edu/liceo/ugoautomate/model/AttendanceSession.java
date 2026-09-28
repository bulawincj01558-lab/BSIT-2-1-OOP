package edu.liceo.ugoautomate.model;

import java.time.LocalDateTime;

/**
 * An authorized attendance activity (class session or event) for which an
 * administrator can generate a QR code.
 * <p>
 * The {@code qrNonce} binds issued QR codes to this session: regenerating the
 * nonce immediately invalidates every previously printed or displayed code.
 */
public class AttendanceSession {

    /** Students may check in this many minutes before the scheduled start. */
    public static final int EARLY_CHECK_IN_MINUTES = 15;

    private long id;
    private String title;
    private SessionType sessionType;
    private String venue;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String qrNonce;
    private boolean active = true;
    private Long createdBy;
    private LocalDateTime createdAt;
    private int attendeeCount;

    /**
     * @param time the moment to test
     * @return true when the session is active and {@code time} falls inside the
     *         check-in window [start - early grace, end]
     */
    public boolean isOpenAt(LocalDateTime time) {
        return active
                && !time.isBefore(checkInOpensAt())
                && !time.isAfter(endTime);
    }

    public LocalDateTime checkInOpensAt() {
        return startTime.minusMinutes(EARLY_CHECK_IN_MINUTES);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getQrNonce() {
        return qrNonce;
    }

    public void setQrNonce(String qrNonce) {
        this.qrNonce = qrNonce;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** @return number of attendance records (populated by list queries) */
    public int getAttendeeCount() {
        return attendeeCount;
    }

    public void setAttendeeCount(int attendeeCount) {
        this.attendeeCount = attendeeCount;
    }

    @Override
    public String toString() {
        return title;
    }
}
