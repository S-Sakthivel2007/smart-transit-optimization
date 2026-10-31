package org.vetrinagar.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated passenger demand batch for a specific (pickup -> destination) corridor.
 * Fulfills two core data structure requirements:
 * 1. Uses an ArrayDeque for FIFO passenger request processing within the group.
 * 2. Provides deterministic keys for the PriorityQueue ranking (count DESC, arrival ASC, batchId ASC).
 */
public class DemandBatch implements Comparable<DemandBatch> {
    private final String batchId;
    private final String pickupStopId;
    private final String destinationStopId;
    private int passengerCount;
    private int originalCount;
    private final long arrivalTimestamp; // Epoch millis for deterministic arrival ordering
    private final String arrivalTimeFormatted;
    private String remarks;
    private DemandStatus status;

    // FIFO queue of individual passenger requests within this corridor
    private final ArrayDeque<DemandRequest> requestQueue;

    public DemandBatch(String batchId, String pickupStopId, String destinationStopId,
                       int passengerCount, long arrivalTimestamp, String arrivalTimeFormatted,
                       String remarks, DemandStatus status) {
        if (passengerCount < 0) {
            throw new IllegalArgumentException("Passenger count cannot be negative");
        }
        this.batchId = Objects.requireNonNull(batchId, "batchId required").trim();
        this.pickupStopId = Objects.requireNonNull(pickupStopId, "pickupStopId required").trim();
        this.destinationStopId = Objects.requireNonNull(destinationStopId, "destinationStopId required").trim();
        this.passengerCount = passengerCount;
        this.originalCount = passengerCount;
        this.arrivalTimestamp = arrivalTimestamp;
        this.arrivalTimeFormatted = arrivalTimeFormatted != null ? arrivalTimeFormatted : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        this.remarks = remarks != null ? remarks : "Demand cluster";
        this.status = status != null ? status : DemandStatus.WAITING;
        this.requestQueue = new ArrayDeque<>();
    }

    public DemandBatch(String batchId, String pickupStopId, String destinationStopId, int passengerCount, String remarks) {
        this(batchId, pickupStopId, destinationStopId, passengerCount, System.currentTimeMillis(),
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), remarks, DemandStatus.WAITING);
    }

    /**
     * Appends an individual passenger request to this batch's FIFO queue and aggregates the count.
     */
    public synchronized void addRequest(DemandRequest request) {
        if (request == null) return;
        request.setAssignedBatchId(this.batchId);
        requestQueue.addLast(request);
        this.passengerCount += request.getPassengerCount();
        this.originalCount += request.getPassengerCount();
        if (this.status == DemandStatus.COMPLETED || this.status == DemandStatus.ALLOCATED) {
            this.status = DemandStatus.WAITING;
        }
    }

    /**
     * Boards up to the given bus capacity using FIFO order from the ArrayDeque.
     * Preserves any partially unserved request at the head of the queue.
     * Returns the actual number of passengers boarded.
     */
    public synchronized int boardPassengers(int capacity) {
        if (capacity <= 0 || passengerCount <= 0) {
            return 0;
        }

        int boarded = Math.min(passengerCount, capacity);
        int remainingToBoard = boarded;

        // Drain requests from the FIFO queue
        List<DemandRequest> servedList = new ArrayList<>();
        while (remainingToBoard > 0 && !requestQueue.isEmpty()) {
            DemandRequest req = requestQueue.peekFirst();
            if (req.getPassengerCount() <= remainingToBoard) {
                remainingToBoard -= req.getPassengerCount();
                req.setPassengerCount(0);
                req.setStatus(DemandStatus.COMPLETED);
                requestQueue.pollFirst();
                servedList.add(req);
            } else {
                // Partial fulfillment of this individual request
                req.setPassengerCount(req.getPassengerCount() - remainingToBoard);
                req.setStatus(DemandStatus.PARTIALLY_ALLOCATED);
                remainingToBoard = 0;
            }
        }

        this.passengerCount -= boarded;

        if (this.passengerCount == 0) {
            this.status = DemandStatus.COMPLETED;
        } else {
            // Retain unserved passengers as pending with original arrival order
            this.status = DemandStatus.PARTIALLY_ALLOCATED;
        }

        return boarded;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getPickupStopId() {
        return pickupStopId;
    }

    public String getDestinationStopId() {
        return destinationStopId;
    }

    public synchronized int getPassengerCount() {
        return passengerCount;
    }

    public synchronized void setPassengerCount(int count) {
        this.passengerCount = Math.max(0, count);
    }

    public int getOriginalCount() {
        return originalCount;
    }

    public long getArrivalTimestamp() {
        return arrivalTimestamp;
    }

    public String getArrivalTimeFormatted() {
        return arrivalTimeFormatted;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public DemandStatus getStatus() {
        return status;
    }

    public void setStatus(DemandStatus status) {
        this.status = status;
    }

    public ArrayDeque<DemandRequest> getRequestQueue() {
        return requestQueue;
    }

    /**
     * Natural ordering for PriorityQueue:
     * 1. Higher passenger count has higher priority (DESC).
     * 2. If passenger counts are equal, earlier arrival timestamp has higher priority (ASC).
     * 3. Deterministic tie-breaker on batchId (ASC).
     */
    @Override
    public int compareTo(DemandBatch other) {
        if (other == null) return -1;
        // Priority 1: Passenger count descending (higher count first)
        int countCmp = Integer.compare(other.passengerCount, this.passengerCount);
        if (countCmp != 0) {
            return countCmp;
        }
        // Priority 2: Arrival order ascending (earlier arrival first)
        int timeCmp = Long.compare(this.arrivalTimestamp, other.arrivalTimestamp);
        if (timeCmp != 0) {
            return timeCmp;
        }
        // Priority 3: Deterministic tie-breaker
        return this.batchId.compareTo(other.batchId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DemandBatch that)) return false;
        return batchId.equals(that.batchId);
    }

    @Override
    public int hashCode() {
        return batchId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s | Waiting: %d/%d pax | Time: %s | Status: %s (%s)",
                batchId, pickupStopId, destinationStopId, passengerCount, originalCount,
                arrivalTimeFormatted, status, remarks);
    }
}
