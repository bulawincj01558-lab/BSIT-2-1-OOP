package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.GuestVisitDao;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.model.VisitStatus;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.QrToken;
import edu.liceo.ugoautomate.security.QrTokenService;
import edu.liceo.ugoautomate.security.QrTokenType;
import edu.liceo.ugoautomate.util.AppConfig;
import edu.liceo.ugoautomate.util.Validation;
import edu.liceo.ugoautomate.util.ValidationException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class GuestVisitService {
    private final GuestVisitDao visitDao;
    private final QrTokenService qrTokens;
    private final AccessControl access;
    private final Clock clock;

    public GuestVisitService(GuestVisitDao visitDao, QrTokenService qrTokens, AccessControl access, Clock clock) {
        this.visitDao = visitDao;
        this.qrTokens = qrTokens;
        this.access = access;
        this.clock = clock;
    }

    public GuestVisit registerVisit(String purpose, String personToVisit, LocalDate visitDate) {
        User guest = access.requireRole(Role.GUEST);
        LocalDate today = LocalDate.now(clock);
        if (visitDate == null) {
            throw new ValidationException("Visit date is required.");
        }
        if (visitDate.isBefore(today)) {
            throw new ValidationException("Visit date cannot be in the past.");
        }
        if (visitDate.isAfter(today.plusDays(AppConfig.MAX_VISIT_DAYS_AHEAD))) {
            throw new ValidationException("Visits can be registered at most " + AppConfig.MAX_VISIT_DAYS_AHEAD + " days ahead.");
        }
        GuestVisit visit = new GuestVisit();
        visit.setGuestId(guest.getId());
        visit.setPurpose(Validation.required(purpose, "Purpose of visit", 200));
        visit.setPersonToVisit(Validation.required(personToVisit, "Person/office to visit", 100));
        visit.setVisitDate(visitDate);
        visit.setStatus(VisitStatus.PENDING);
        visit.setRegisteredAt(LocalDateTime.now(clock).withNano(0));
        visitDao.create(visit);
        visit.setGuestName(guest.getFullName());
        visit.setGuestContact(guest.getContactNumber());
        return visit;
    }

    public List<GuestVisit> myVisits() {
        User guest = access.requireRole(Role.GUEST);
        return visitDao.findByGuest(guest.getId());
    }

    public QrToken issueGuestPass(long visitId) {
        User guest = access.requireRole(Role.GUEST);
        GuestVisit visit = find(visitId);
        if (visit.getGuestId() != guest.getId()) {
            throw new ServiceException("You can only view passes for your own visits.");
        }
        if (visit.getStatus() != VisitStatus.PENDING) {
            throw new ServiceException("A guest pass is only available for pending visits (this visit is "
                    + visit.getStatus().getLabel().toLowerCase() + ").");
        }
        if (visit.getVisitDate().isBefore(LocalDate.now(clock))) {
            throw new ServiceException("This visit date has passed. Please register a new visit.");
        }
        LocalDateTime endOfVisitDay = visit.getVisitDate().plusDays(1).atStartOfDay();
        return qrTokens.issue(QrTokenType.GUEST_PASS, visit.getId(), qrTokens.toInstant(endOfVisitDay));
    }

    public void cancelVisit(long visitId) {
        User user = access.requireRole(Role.GUEST, Role.ADMIN);
        GuestVisit visit = find(visitId);
        if (user.hasRole(Role.GUEST) && visit.getGuestId() != user.getId()) {
            throw new ServiceException("You can only cancel your own visits.");
        }
        if (visit.getStatus() != VisitStatus.PENDING) {
            throw new ServiceException("Only pending visits can be cancelled.");
        }
        visitDao.updateStatus(visitId, VisitStatus.CANCELLED, LocalDateTime.now(clock).withNano(0));
    }

    public List<GuestVisit> listVisits(LocalDate date) {
        access.requireRole(Role.ADMIN);
        return visitDao.findAll(date);
    }

    public void checkOut(long visitId) {
        access.requireRole(Role.ADMIN);
        GuestVisit visit = find(visitId);
        if (visit.getStatus() != VisitStatus.CHECKED_IN) {
            throw new ServiceException("Only checked-in visits can be checked out.");
        }
        visitDao.updateStatus(visitId, VisitStatus.CHECKED_OUT, LocalDateTime.now(clock).withNano(0));
    }

    private GuestVisit find(long visitId) {
        return visitDao.findById(visitId).orElseThrow(() -> new ServiceException("Visit not found."));
    }
}
