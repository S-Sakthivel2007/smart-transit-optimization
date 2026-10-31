package org.vetrinagar.model;

/**
 * Road operational status.
 */
public enum RoadStatus {
    OPEN,
    CLOSED;

    public static RoadStatus fromString(String val) {
        if (val == null) return OPEN;
        return val.trim().equalsIgnoreCase("closed") ? CLOSED : OPEN;
    }
}
