package edu.liceo.ugoautomate.model;

import java.time.LocalDateTime;
import java.util.Objects;

public abstract class User {
    private long id;
    private final Role role;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String contactNumber;
    private boolean active = true;
    private LocalDateTime createdAt;

    protected User(Role role) {
        this.role = Objects.requireNonNull(role, "role");
    }

    public abstract String getSummary();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean hasRole(Role other) {
        return role == other;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        return id != 0 && id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return role.getLabel() + " #" + id + " " + getFullName() + " <" + email + ">";
    }
}
