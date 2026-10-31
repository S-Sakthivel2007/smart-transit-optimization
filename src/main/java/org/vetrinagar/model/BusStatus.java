package org.vetrinagar.model;

/**
 * Operational status of a relief bus.
 */
public enum BusStatus {
    AVAILABLE,
    ON_TRIP,
    BROKEN;

    public static BusStatus fromString(String val) {
        if (val == null) return AVAILABLE;
        val = val.trim().toLowerCase();
        if (val.contains("trip") || val.contains("route")) return ON_TRIP;
        if (val.contains("broke") || val.contains("maint")) return BROKEN;
        return AVAILABLE;
    }
}
