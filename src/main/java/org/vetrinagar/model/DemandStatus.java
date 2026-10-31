package org.vetrinagar.model;

/**
 * Status of passenger relief demand.
 */
public enum DemandStatus {
    WAITING,
    PARTIALLY_ALLOCATED,
    ALLOCATED,
    COMPLETED,
    CANCELLED;

    public static DemandStatus fromString(String val) {
        if (val == null) return WAITING;
        val = val.trim().toLowerCase();
        if (val.contains("part")) return PARTIALLY_ALLOCATED;
        if (val.contains("alloc")) return ALLOCATED;
        if (val.contains("compl")) return COMPLETED;
        if (val.contains("canc")) return CANCELLED;
        return WAITING;
    }
}
