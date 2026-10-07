package edu.liceo.ugoautomate.security;

import edu.liceo.ugoautomate.model.User;

import java.util.Optional;

public class SessionManager {
    private volatile User currentUser;

    public void start(User user) {
        this.currentUser = user;
    }

    public void end() {
        this.currentUser = null;
    }

    public Optional<User> current() {
        return Optional.ofNullable(currentUser);
    }

    public void refresh(User updated) {
        User current = currentUser;
        if (current != null && current.getId() == updated.getId()) {
            this.currentUser = updated;
        }
    }
}
