package org.vetrinagar.model;

/**
 * Road directionality type.
 */
public enum RoadType {
    TWO_WAY,
    ONE_WAY;

    public static RoadType fromString(String val) {
        if (val == null) return TWO_WAY;
        return val.trim().equalsIgnoreCase("one_way") ? ONE_WAY : TWO_WAY;
    }
}
