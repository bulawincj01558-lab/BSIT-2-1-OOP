package edu.liceo.ugoautomate.security;

import edu.liceo.ugoautomate.model.User;

import java.util.Optional;

/**
 * Holds the currently signed-in user of this desktop client. Thread-safe so
 * background workers can read the session.
 */
public final class SessionManager {

    private volatile User currentUser;

    public void start(User user) {
        this.currentUser = user;
    }

    public void end() {
        this.currentUser = null;
    }

    public Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
