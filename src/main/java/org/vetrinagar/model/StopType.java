package org.vetrinagar.model;

/**
 * Enumeration for Stop types.
 */
public enum StopType {
    DEPOT,
    STOP;

    public static StopType fromString(String type) {
        if (type == null) return STOP;
        return type.trim().equalsIgnoreCase("depot") ? DEPOT : STOP;
    }
}
