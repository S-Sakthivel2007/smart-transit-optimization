package org.vetrinagar.model;

/**
 * Roles for system authentication.
 */
public enum Role {
    SUPERVISOR,
    PASSENGER;

    public static Role fromString(String val) {
        if (val == null) return PASSENGER;
        return val.trim().equalsIgnoreCase("supervisor") ? SUPERVISOR : PASSENGER;
    }
}
