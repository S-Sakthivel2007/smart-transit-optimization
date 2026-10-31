package org.vetrinagar.model;

import java.util.Objects;

/**
 * Abstract base class representing a public transit relief bus.
 * Demonstrates Encapsulation by shielding internal occupancy and status.
 * Demonstrates Polymorphism through abstract canUseRoad(Road road).
 */
public abstract class Bus {
    private final String busId;
    private final String busType;
    private final int capacity;
    private String currentLocation; // stopId
    private BusStatus status;
    private int passengersOnboard;
    private String currentTripId;

    public Bus(String busId, String busType, int capacity, String currentLocation, BusStatus status) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Bus capacity must be positive (> 0)");
        }
        this.busId = Objects.requireNonNull(busId, "busId cannot be null").trim();
        this.busType = Objects.requireNonNull(busType, "busType cannot be null").trim();
        this.capacity = capacity;
        this.currentLocation = Objects.requireNonNull(currentLocation, "currentLocation cannot be null").trim();
        this.status = status != null ? status : BusStatus.AVAILABLE;
        this.passengersOnboard = 0;
        this.currentTripId = null;
    }

    public String getBusId() {
        return busId;
    }

    public String getBusType() {
        return busType;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = Objects.requireNonNull(currentLocation, "currentLocation cannot be null").trim();
    }

    public BusStatus getStatus() {
        return status;
    }

    public void setStatus(BusStatus status) {
        this.status = status != null ? status : BusStatus.AVAILABLE;
        if (this.status != BusStatus.ON_TRIP) {
            this.currentTripId = null;
        }
    }

    public int getPassengersOnboard() {
        return passengersOnboard;
    }

    public String getCurrentTripId() {
        return currentTripId;
    }

    public void setCurrentTripId(String tripId) {
        this.currentTripId = tripId;
    }

    public boolean isAvailable() {
        return this.status == BusStatus.AVAILABLE;
    }

    /**
     * Encapsulated method to safely board passengers within bus capacity.
     * Returns the actual number of passengers boarded.
     */
    public synchronized int boardPassengers(int passengerCount) {
        if (passengerCount <= 0) {
            return 0;
        }
        if (status != BusStatus.AVAILABLE && status != BusStatus.ON_TRIP) {
            throw new IllegalStateException("Bus " + busId + " is " + status + " and cannot board passengers.");
        }
        int availableSeats = capacity - passengersOnboard;
        int boarded = Math.min(passengerCount, availableSeats);
        this.passengersOnboard += boarded;
        this.status = BusStatus.ON_TRIP;
        return boarded;
    }

    /**
     * Encapsulated method to complete a relief trip, update stop location, and release bus.
     */
    public synchronized void completeTrip(String destinationStopId) {
        this.passengersOnboard = 0;
        this.currentLocation = Objects.requireNonNull(destinationStopId, "destinationStopId cannot be null");
        this.status = BusStatus.AVAILABLE;
        this.currentTripId = null;
    }

    /**
     * Encapsulated method to release a bus back to AVAILABLE state.
     */
    public synchronized void releaseBus() {
        this.passengersOnboard = 0;
        this.status = BusStatus.AVAILABLE;
        this.currentTripId = null;
    }

    /**
     * Polymorphic method to determine whether this bus can navigate a specific road,
     * taking width, weight limits, and bridge restrictions into account.
     */
    public abstract boolean canUseRoad(Road road);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Bus bus)) return false;
        return busId.equals(bus.busId);
    }

    @Override
    public int hashCode() {
        return busId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (Cap: %d, At: %s, Status: %s, Load: %d/%d)",
                busId, busType, capacity, currentLocation, status, passengersOnboard, capacity);
    }
}
