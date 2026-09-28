package edu.liceo.ugoautomate.model;

import java.time.LocalDateTime;

/**
 * Base class for every account in the system. Concrete subclasses add the
 * role-specific information ({@link Student}, {@link Guest}, {@link Administrator}).
 */
public abstract class User {

    private long id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String contactNumber;
    private boolean active = true;
    private LocalDateTime createdAt;

    protected User() {
    }

    /** @return the role this account type represents */
    public abstract Role getRole();

    /**
     * @return the identifier shown to staff when verifying this user
     *         (student number for students, username otherwise)
     */
    public String getDisplayIdentifier() {
        return username;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + getRole().getDisplayName() + ")";
    }
}
