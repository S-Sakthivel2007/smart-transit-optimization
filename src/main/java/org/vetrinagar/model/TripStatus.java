package org.vetrinagar.model;

/**
 * Lifecycle status of a relief trip.
 */
public enum TripStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    REQUIRES_ATTENTION,
    CANCELLED;

    public static TripStatus fromString(String val) {
        if (val == null) return PLANNED;
        val = val.trim().toLowerCase();
        if (val.contains("prog") || val.contains("route")) return IN_PROGRESS;
        if (val.contains("comp")) return COMPLETED;
        if (val.contains("attent") || val.contains("broken")) return REQUIRES_ATTENTION;
        if (val.contains("canc")) return CANCELLED;
        return PLANNED;
    }
}
