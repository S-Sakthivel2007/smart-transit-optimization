package org.vetrinagar.model;

import java.util.Objects;

/**
 * User account representation for authentication and role-based access.
 */
public class User {
    private final String username;
    private final String password; // Simple password check for course review
    private final Role role;
    private final String fullName;

    public User(String username, String password, Role role, String fullName) {
        this.username = Objects.requireNonNull(username, "username required").trim();
        this.password = Objects.requireNonNull(password, "password required");
        this.role = role != null ? role : Role.PASSENGER;
        this.fullName = fullName != null ? fullName.trim() : username;
    }

    public String getUsername() {
        return username;
    }

    public boolean checkPassword(String candidate) {
        return this.password.equals(candidate);
    }

    public Role getRole() {
        return role;
    }

    public boolean isSupervisor() {
        return role == Role.SUPERVISOR;
    }

    public boolean isPassenger() {
        return role == Role.PASSENGER;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public String toString() {
        return String.format("%s (%s - %s)", fullName, username, role);
    }
}
