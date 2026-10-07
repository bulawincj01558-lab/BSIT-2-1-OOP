package edu.liceo.ugoautomate.security;

import edu.liceo.ugoautomate.model.Role;
import edu.liceo.ugoautomate.model.User;

import java.util.Arrays;

public class AccessControl {
    private final SessionManager session;

    public AccessControl(SessionManager session) {
        this.session = session;
    }

    public User requireLoggedIn() {
        return session.current()
                .orElseThrow(() -> new AccessDeniedException("Please log in to use this feature."));
    }

    public User requireRole(Role... allowed) {
        User user = requireLoggedIn();
        boolean permitted = Arrays.stream(allowed).anyMatch(user::hasRole);
        if (!permitted) {
            throw new AccessDeniedException("Your account (" + user.getRole().getLabel()
                    + ") is not allowed to use this feature.");
        }
        return user;
    }
}
