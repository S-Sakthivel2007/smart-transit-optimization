package org.vetrinagar.model;

import java.util.Objects;

/**
 * Represents a directed or undirected road segment in the transit graph.
 */
public class Road {
    private final int roadId;
    private final String fromStopId;
    private final String toStopId;
    private int travelTime; // in minutes (positive)
    private RoadStatus status;
    private final RoadType roadType;
    private String weightLimit; // "All", "MiniBus Only", "No Narrow Lanes"

    public Road(int roadId, String fromStopId, String toStopId, int travelTime, RoadStatus status, RoadType roadType, String weightLimit) {
        if (travelTime <= 0) {
            throw new IllegalArgumentException("Road travel time must be positive (> 0), got: " + travelTime);
        }
        this.roadId = roadId;
        this.fromStopId = Objects.requireNonNull(fromStopId, "fromStopId required").trim();
        this.toStopId = Objects.requireNonNull(toStopId, "toStopId required").trim();
        this.travelTime = travelTime;
        this.status = status != null ? status : RoadStatus.OPEN;
        this.roadType = roadType != null ? roadType : RoadType.TWO_WAY;
        this.weightLimit = weightLimit != null ? weightLimit.trim() : "All";
    }

    public int getRoadId() {
        return roadId;
    }

    public String getFromStopId() {
        return fromStopId;
    }

    public String getToStopId() {
        return toStopId;
    }

    public int getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(int travelTime) {
        if (travelTime <= 0) {
            throw new IllegalArgumentException("Travel time must be positive (> 0)");
        }
        this.travelTime = travelTime;
    }

    public RoadStatus getStatus() {
        return status;
    }

    public void setStatus(RoadStatus status) {
        this.status = status != null ? status : RoadStatus.OPEN;
    }

    public boolean isOpen() {
        return this.status == RoadStatus.OPEN;
    }

    public RoadType getRoadType() {
        return roadType;
    }

    public boolean isTwoWay() {
        return roadType == RoadType.TWO_WAY;
    }

    public String getWeightLimit() {
        return weightLimit;
    }

    public void setWeightLimit(String weightLimit) {
        this.weightLimit = weightLimit != null ? weightLimit : "All";
    }

    @Override
    public String toString() {
        return String.format("Road #%d: %s -> %s (%d mins, %s, %s, restriction=%s)",
                roadId, fromStopId, toStopId, travelTime, roadType, status, weightLimit);
    }
}
