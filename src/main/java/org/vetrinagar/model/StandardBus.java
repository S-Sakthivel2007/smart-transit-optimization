package org.vetrinagar.model;

/**
 * Concrete subclass representing a full-sized StandardBus.
 * High passenger capacity (40-60 seats), but restricted from narrow lanes
 * and weight-limited bridges.
 */
public class StandardBus extends Bus {

    public StandardBus(String busId, int capacity, String currentLocation, BusStatus status) {
        super(busId, "StandardBus", capacity, currentLocation, status);
    }

    public StandardBus(String busId, int capacity, String currentLocation) {
        this(busId, capacity, currentLocation, BusStatus.AVAILABLE);
    }

    /**
     * StandardBus cannot traverse narrow historic lanes, alleys, or "MiniBus Only" bridges.
     */
    @Override
    public boolean canUseRoad(Road road) {
        if (road == null || !road.isOpen()) {
            return false;
        }
        String limit = road.getWeightLimit();
        if (limit != null) {
            String lower = limit.toLowerCase();
            if (lower.contains("minibus only") || lower.contains("narrow") || lower.contains("restricted")) {
                return false;
            }
        }
        return true;
    }
}
