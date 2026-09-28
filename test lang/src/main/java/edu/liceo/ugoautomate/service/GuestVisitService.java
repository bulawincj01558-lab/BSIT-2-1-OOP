package edu.liceo.ugoautomate.service;

import edu.liceo.ugoautomate.dao.GuestVisitDao;
import edu.liceo.ugoautomate.model.Guest;
import edu.liceo.ugoautomate.model.GuestVisit;
import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.User;
import edu.liceo.ugoautomate.model.VisitStatus;
import edu.liceo.ugoautomate.security.AccessControl;
import edu.liceo.ugoautomate.security.QrTokenService;
import edu.liceo.ugoautomate.security.QrTokenType;
import edu.liceo.ugoautomate.util.DateTimeUtil;
import edu.liceo.ugoautomate.util.Validators;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Guest visit registration before campus entry (F-4.2) and guest visit logs (F-5.4).
 * <p>
 * Visits are registered on-site for the current day only (online
 * pre-registration is out of scope). A visit pass is a signed QR code bound to
 * the visit's nonce and valid until the end of the visit day; it can be used
 * for one check-in.
 */
public class GuestVisitService {

    public static final int RECENT_LIMIT = 500;

    private final GuestVisitDao visitDao;
    private final AccessControl access;
    private final QrTokenService qrTokens;
    private final Clock clock;

    public GuestVisitService(GuestVisitDao visitDao, AccessControl access, QrTokenService qrTokens, Clock clock) {
        this.visitDao = visitDao;
        this.access = access;
        this.qrTokens = qrTokens;
        this.clock = clock;
    }

    /** Registers a visit for today for the signed-in guest. */
    public GuestVisit registerVisit(String purpose, String personToVisit) {
        Guest guest = access.requireGuest();
        String cleanPurpose = Validators.requireText(purpose, "Purpose of visit", 200);
        String cleanPerson = Validators.requireText(personToVisit, "Person / office to visit", 120);
        LocalDate today = LocalDate.now(clock);
        if (visitDao.findRegisteredForGuestOn(guest.getId(), today).isPresent()) {
            throw new ServiceException("You already have a registered visit for today. "
                    + "Show its visit pass at the gate, or cancel it to register a new one.");
        }
        GuestVisit visit = new GuestVisit();
        visit.setGuestUserId(guest.getId());
        visit.setGuestName(guest.getFullName());
        visit.setGuestContact(guest.getContactNumber());
        visit.setPurpose(cleanPurpose);
        visit.setPersonToVisit(cleanPerson);
        visit.setVisitDate(today);
        visit.setStatus(VisitStatus.REGISTERED);
        visit.setPassNonce(QrTokenService.newNonce());
        visit.setRegisteredAt(LocalDateTime.now(clock));
        visitDao.insert(visit);
        return visit;
    }

    public List<GuestVisit> myVisits() {
        Guest guest = access.requireGuest();
        return visitDao.findByGuest(guest.getId());
    }

    /** @return the signed visit-pass payload for one of the guest's own visits */
    public String visitPassPayload(long visitId) {
        Guest guest = access.requireGuest();
        GuestVisit visit = requireVisit(visitId);
        if (visit.getGuestUserId() != guest.getId()) {
            throw new ServiceException("You can only view your own visit passes.");
        }
        if (visit.getStatus() != VisitStatus.REGISTERED) {
            throw new ServiceException("This visit is already " + visit.getStatus().getDisplayName().toLowerCase()
                    + "; its pass can no longer be used.");
        }
        if (!visit.getVisitDate().equals(LocalDate.now(clock))) {
            throw new ServiceException("This visit pass was only valid on " + DateTimeUtil.format(visit.getVisitDate()) + ".");
        }
        return qrTokens.issue(QrTokenType.VISIT_PASS, visit.getId(), visit.getPassNonce(),
                DateTimeUtil.endOfDay(visit.getVisitDate()).atZone(clock.getZone()).toInstant());
    }

    /** Cancels a registered visit. Guests may cancel their own; administrators any. */
    public void cancelVisit(long visitId) {
        User user = access.requireRole(Role.GUEST, Role.ADMIN);
        GuestVisit visit = requireVisit(visitId);
        if (user.getRole() == Role.GUEST && visit.getGuestUserId() != user.getId()) {
            throw new ServiceException("You can only cancel your own visits.");
        }
        if (visit.getStatus() != VisitStatus.REGISTERED) {
            throw new ServiceException("Only visits that have not been checked in can be cancelled.");
        }
        visitDao.updateStatus(visitId, VisitStatus.CANCELLED);
    }

    /** @param date visit date, or {@code null} for the most recent registrations */
    public List<GuestVisit> listVisits(LocalDate date) {
        access.requireAdmin();
        return date == null ? visitDao.findRecent(RECENT_LIMIT) : visitDao.findByDate(date);
    }

    private GuestVisit requireVisit(long visitId) {
        return visitDao.findById(visitId)
                .orElseThrow(() -> new ServiceException("The selected visit no longer exists."));
    }
}
