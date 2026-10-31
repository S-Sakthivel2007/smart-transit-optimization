package org.vetrinagar.model;

import java.util.Objects;

/**
 * Represents a transit stop or depot in Vetrinagar.
 */
public class Stop {
    private final String stopId;
    private final String stopName;
    private final StopType stopType;
    private final String locationDesc;
    private final int mapX;
    private final int mapY;

    public Stop(String stopId, String stopName, StopType stopType, String locationDesc, int mapX, int mapY) {
        this.stopId = Objects.requireNonNull(stopId, "stopId cannot be null").trim();
        this.stopName = Objects.requireNonNull(stopName, "stopName cannot be null").trim();
        this.stopType = stopType != null ? stopType : StopType.STOP;
        this.locationDesc = locationDesc != null ? locationDesc : "";
        this.mapX = mapX;
        this.mapY = mapY;
    }

    public String getStopId() {
        return stopId;
    }

    public String getStopName() {
        return stopName;
    }

    public StopType getStopType() {
        return stopType;
    }

    public boolean isDepot() {
        return stopType == StopType.DEPOT;
    }

    public String getLocationDesc() {
        return locationDesc;
    }

    public int getMapX() {
        return mapX;
    }

    public int getMapY() {
        return mapY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stop stop)) return false;
        return stopId.equals(stop.stopId);
    }

    @Override
    public int hashCode() {
        return stopId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", stopId, stopName, stopType);
    }
}
