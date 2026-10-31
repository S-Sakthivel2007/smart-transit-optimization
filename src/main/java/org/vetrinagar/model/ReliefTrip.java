package org.vetrinagar.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a relief service trip consisting of two routing segments:
 * Segment 1: Bus current location -> Pickup stop (dispatch)
 * Segment 2: Pickup stop -> Destination stop (passenger transit)
 */
public class ReliefTrip {
    private final String tripId;
    private final String batchId;
    private String busId;
    private final String pickupStopId;
    private final String destinationStopId;
    private final int passengersAllocated;
    private final int busCapacity;

    // Segment 1 (Bus origin -> Pickup stop)
    private List<String> segment1Path;
    private int segment1Time;

    // Segment 2 (Pickup stop -> Destination stop)
    private List<String> segment2Path;
    private int segment2Time;

    private int totalTravelTime;
    private TripStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public ReliefTrip(String tripId, String batchId, String busId,
                      String pickupStopId, String destinationStopId,
                      int passengersAllocated, int busCapacity,
                      List<String> segment1Path, int segment1Time,
                      List<String> segment2Path, int segment2Time,
                      TripStatus status) {
        this.tripId = Objects.requireNonNull(tripId, "tripId required").trim();
        this.batchId = Objects.requireNonNull(batchId, "batchId required").trim();
        this.busId = Objects.requireNonNull(busId, "busId required").trim();
        this.pickupStopId = Objects.requireNonNull(pickupStopId, "pickupStopId required").trim();
        this.destinationStopId = Objects.requireNonNull(destinationStopId, "destinationStopId required").trim();
        this.passengersAllocated = passengersAllocated;
        this.busCapacity = busCapacity;
        this.segment1Path = segment1Path != null ? new ArrayList<>(segment1Path) : new ArrayList<>();
        this.segment1Time = Math.max(0, segment1Time);
        this.segment2Path = segment2Path != null ? new ArrayList<>(segment2Path) : new ArrayList<>();
        this.segment2Time = Math.max(0, segment2Time);
        this.totalTravelTime = this.segment1Time + this.segment2Time;
        this.status = status != null ? status : TripStatus.PLANNED;
        this.createdAt = LocalDateTime.now();
        this.completedAt = null;
    }

    public String getTripId() {
        return tripId;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public String getPickupStopId() {
        return pickupStopId;
    }

    public String getDestinationStopId() {
        return destinationStopId;
    }

    public int getPassengersAllocated() {
        return passengersAllocated;
    }

    public int getBusCapacity() {
        return busCapacity;
    }

    public List<String> getSegment1Path() {
        return Collections.unmodifiableList(segment1Path);
    }

    public void setSegment1(List<String> path, int time) {
        this.segment1Path = path != null ? new ArrayList<>(path) : new ArrayList<>();
        this.segment1Time = Math.max(0, time);
        this.totalTravelTime = this.segment1Time + this.segment2Time;
    }

    public int getSegment1Time() {
        return segment1Time;
    }

    public List<String> getSegment2Path() {
        return Collections.unmodifiableList(segment2Path);
    }

    public void setSegment2(List<String> path, int time) {
        this.segment2Path = path != null ? new ArrayList<>(path) : new ArrayList<>();
        this.segment2Time = Math.max(0, time);
        this.totalTravelTime = this.segment1Time + this.segment2Time;
    }

    public int getSegment2Time() {
        return segment2Time;
    }

    public int getTotalTravelTime() {
        return totalTravelTime;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
        if (status == TripStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getFullPathString() {
        StringBuilder sb = new StringBuilder();
        if (!segment1Path.isEmpty()) {
            sb.append(String.join(" -> ", segment1Path));
        }
        if (!segment2Path.isEmpty()) {
            if (sb.length() > 0) {
                // If segment 1 ended at pickup, avoid duplicating the pickup stop name
                sb.append(" => [BOARD] => ");
                sb.append(String.join(" -> ", segment2Path));
            } else {
                sb.append(String.join(" -> ", segment2Path));
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] Bus: %s | Demand: %s | %s -> %s | Pax: %d/%d | Total: %d mins | %s",
                tripId, busId, batchId, pickupStopId, destinationStopId,
                passengersAllocated, busCapacity, totalTravelTime, status);
    }
}
