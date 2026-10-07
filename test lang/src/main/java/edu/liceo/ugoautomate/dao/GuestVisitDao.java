package edu.liceo.ugoautomate.dao;

import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.model.VisitStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GuestVisitDao {
    long create(GuestVisit visit);

    Optional<GuestVisit> findById(long id);

    List<GuestVisit> findByGuest(long guestId);

    List<GuestVisit> findAll(LocalDate date);

    void updateStatus(long visitId, VisitStatus status, LocalDateTime at);
}
