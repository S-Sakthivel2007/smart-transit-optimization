package org.vetrinagar.model;

/**
 * Concrete subclass representing a compact MiniBus.
 * Compact dimensions allow it to traverse narrow heritage bridges and alleys.
 */
public class MiniBus extends Bus {

    public MiniBus(String busId, int capacity, String currentLocation, BusStatus status) {
        super(busId, "MiniBus", capacity, currentLocation, status);
    }

    public MiniBus(String busId, int capacity, String currentLocation) {
        this(busId, capacity, currentLocation, BusStatus.AVAILABLE);
    }

    /**
     * MiniBus can navigate standard roads as well as narrow alleys and "MiniBus Only" corridors.
     */
    @Override
    public boolean canUseRoad(Road road) {
        if (road == null || !road.isOpen()) {
            return false;
        }
        // MiniBus is agile and can traverse all open roads
        return true;
    }
}
