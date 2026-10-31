package org.vetrinagar.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an individual passenger request submitted via the passenger portal.
 */
public class DemandRequest {
    private final String requestId;
    private final String passengerId;
    private final String passengerName;
    private final String pickupStopId;
    private final String destinationStopId;
    private int passengerCount;
    private final int originalCount;
    private final LocalDateTime submittedAt;
    private String assignedBatchId;
    private DemandStatus status;

    public DemandRequest(String requestId, String passengerId, String passengerName,
                         String pickupStopId, String destinationStopId,
                         int passengerCount, LocalDateTime submittedAt) {
        if (passengerCount <= 0) {
            throw new IllegalArgumentException("Passenger count must be positive (> 0)");
        }
        this.requestId = Objects.requireNonNull(requestId, "requestId required").trim();
        this.passengerId = passengerId != null ? passengerId.trim() : "GUEST";
        this.passengerName = passengerName != null ? passengerName.trim() : "Passenger";
        this.pickupStopId = Objects.requireNonNull(pickupStopId, "pickupStopId required").trim();
        this.destinationStopId = Objects.requireNonNull(destinationStopId, "destinationStopId required").trim();
        this.passengerCount = passengerCount;
        this.originalCount = passengerCount;
        this.submittedAt = submittedAt != null ? submittedAt : LocalDateTime.now();
        this.status = DemandStatus.WAITING;
        this.assignedBatchId = null;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public String getPickupStopId() {
        return pickupStopId;
    }

    public String getDestinationStopId() {
        return destinationStopId;
    }

    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = Math.max(0, passengerCount);
    }

    public int getOriginalCount() {
        return originalCount;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getAssignedBatchId() {
        return assignedBatchId;
    }

    public void setAssignedBatchId(String assignedBatchId) {
        this.assignedBatchId = assignedBatchId;
    }

    public DemandStatus getStatus() {
        return status;
    }

    public void setStatus(DemandStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s -> %s (%d passengers, %s)",
                requestId, passengerName, pickupStopId, destinationStopId, passengerCount, status);
    }
}
