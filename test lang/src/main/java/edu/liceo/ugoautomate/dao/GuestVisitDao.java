package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.model.VisitStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Persistence for guest visit registrations.
 */
public interface GuestVisitDao {

    long insert(GuestVisit visit);

    Optional<GuestVisit> findById(long id);

    List<GuestVisit> findByGuest(long guestUserId);

    Optional<GuestVisit> findRegisteredForGuestOn(long guestUserId, LocalDate date);

    List<GuestVisit> findByDate(LocalDate date);

    List<GuestVisit> findRecent(int limit);

    /**
     * Atomically moves a visit from REGISTERED to CHECKED_IN so a visit pass
     * can be used only once, even under concurrent scans.
     *
     * @return true if this call performed the check-in
     */
    boolean markCheckedIn(long visitId, LocalDateTime checkedInAt);

    void updateStatus(long visitId, VisitStatus status);
}
