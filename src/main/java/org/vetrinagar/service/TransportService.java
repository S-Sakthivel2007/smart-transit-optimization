package org.vetrinagar.service;

import org.vetrinagar.db.DatabaseManager;
import org.vetrinagar.graph.DijkstraRouter;
import org.vetrinagar.graph.RouteResult;
import org.vetrinagar.graph.WeightedGraph;
import org.vetrinagar.model.*;
import org.vetrinagar.policy.DefaultDemandPolicy;
import org.vetrinagar.policy.DemandPolicy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core engine composing the transit components:
 * - HashMap for O(1) lookups: stops, buses, trips, corridor batches
 * - PriorityQueue for ranking waiting demand
 * - ArrayDeque for FIFO queue of individual passenger requests within each corridor
 * - WeightedGraph + DijkstraRouter for minimum-time 2-segment routing
 * - DemandPolicy abstraction for dynamic prioritization
 * - DatabaseManager for persistent storage
 */
public class TransportService {
    // Composition of fundamental data structures
    private final Map<String, Stop> stopMap;                      // HashMap: stopId -> Stop
    private final Map<String, Bus> busMap;                        // HashMap: busId -> Bus
    private final Map<String, ReliefTrip> tripMap;                // HashMap: tripId -> ReliefTrip
    private final Map<String, DemandBatch> batchMap;              // HashMap: batchId -> DemandBatch
    private final Map<String, String> corridorToBatchMap;         // HashMap: "pickup:dest" -> batchId
    private final Map<String, DemandRequest> requestMap;          // HashMap: requestId -> DemandRequest

    private final PriorityQueue<DemandBatch> waitingDemandQueue;  // PriorityQueue: ranked by count DESC, arrival ASC
    private final WeightedGraph roadNetwork;                      // Weighted graph of road segments
    private final DijkstraRouter router;                          // Dijkstra shortest-path planner
    private DemandPolicy demandPolicy;                            // Policy abstraction
    private final DatabaseManager dbManager;                      // Database persistence

    private final AtomicInteger tripSequence = new AtomicInteger(100);
    private final AtomicInteger requestSequence = new AtomicInteger(500);

    public TransportService(DatabaseManager dbManager, DemandPolicy demandPolicy) {
        this.dbManager = dbManager != null ? dbManager : new DatabaseManager();
        this.demandPolicy = demandPolicy != null ? demandPolicy : new DefaultDemandPolicy();

        this.stopMap = new ConcurrentHashMap<>();
        this.busMap = new ConcurrentHashMap<>();
        this.tripMap = new ConcurrentHashMap<>();
        this.batchMap = new ConcurrentHashMap<>();
        this.corridorToBatchMap = new ConcurrentHashMap<>();
        this.requestMap = new ConcurrentHashMap<>();

        this.waitingDemandQueue = new PriorityQueue<>(this.demandPolicy.getComparator());
        this.roadNetwork = new WeightedGraph();

        loadFromDatabase();

        this.router = new DijkstraRouter(this.roadNetwork);
    }

    public TransportService() {
        this(new DatabaseManager(), new DefaultDemandPolicy());
    }

    /**
     * Loads stops, roads, buses, demand batches, and relief trips from SQLite.
     */
    private synchronized void loadFromDatabase() {
        // 1. Load Stops
        List<Stop> stops = dbManager.loadAllStops();
        for (Stop s : stops) {
            stopMap.put(s.getStopId(), s);
            roadNetwork.addStop(s);
        }

        // 2. Load Roads into Weighted Graph
        List<Road> roads = dbManager.loadAllRoads();
        for (Road r : roads) {
            roadNetwork.addRoad(r);
        }

        // 3. Load Buses
        List<Bus> buses = dbManager.loadAllBuses();
        for (Bus b : buses) {
            busMap.put(b.getBusId(), b);
        }

        // 4. Load Demand Batches
        List<DemandBatch> batches = dbManager.loadAllDemandBatches();
        for (DemandBatch b : batches) {
            batchMap.put(b.getBatchId(), b);
            String corridorKey = b.getPickupStopId() + "->" + b.getDestinationStopId();
            corridorToBatchMap.put(corridorKey, b.getBatchId());

            if (b.getStatus() == DemandStatus.WAITING || b.getStatus() == DemandStatus.PARTIALLY_ALLOCATED) {
                if (b.getPassengerCount() > 0) {
                    waitingDemandQueue.add(b);
                }
            }
        }

        // 5. Load Relief Trips
        List<ReliefTrip> trips = dbManager.loadAllReliefTrips();
        for (ReliefTrip t : trips) {
            tripMap.put(t.getTripId(), t);
        }
    }

    // =========================================================================
    // PASSENGER PORTAL & DEMAND AGGREGATION
    // =========================================================================

    /**
     * Submits an individual passenger booking request.
     * If an active waiting batch exists for the same (pickup -> destination) corridor,
     * the counts are AGGREGATED into the single batch, using ArrayDeque FIFO batching,
     * and the PriorityQueue ranking is deterministically updated!
     */
    public synchronized DemandRequest submitPassengerDemand(String passengerId, String passengerName,
                                                            String pickupStopId, String destinationStopId,
                                                            int passengerCount) {
        if (!stopMap.containsKey(pickupStopId)) {
            throw new IllegalArgumentException("Unknown pickup stop: " + pickupStopId);
        }
        if (!stopMap.containsKey(destinationStopId)) {
            throw new IllegalArgumentException("Unknown destination stop: " + destinationStopId);
        }
        if (pickupStopId.equals(destinationStopId)) {
            throw new IllegalArgumentException("Pickup and destination cannot be identical.");
        }
        if (passengerCount <= 0) {
            throw new IllegalArgumentException("Passenger count must be greater than zero.");
        }

        String reqId = "REQ-" + requestSequence.incrementAndGet();
        DemandRequest req = new DemandRequest(reqId, passengerId, passengerName, pickupStopId, destinationStopId, passengerCount, LocalDateTime.now());
        requestMap.put(reqId, req);

        String corridorKey = pickupStopId + "->" + destinationStopId;
        String existingBatchId = corridorToBatchMap.get(corridorKey);
        DemandBatch batch = existingBatchId != null ? batchMap.get(existingBatchId) : null;

        // If batch exists and is still open for passenger consolidation
        if (batch != null && (batch.getStatus() == DemandStatus.WAITING || batch.getStatus() == DemandStatus.PARTIALLY_ALLOCATED)) {
            // Remove from PriorityQueue before modifying ranking fields (Rule: remove and reinsert)
            waitingDemandQueue.remove(batch);

            // Add to ArrayDeque FIFO and aggregate passenger count
            batch.addRequest(req);

            // Reinsert into PriorityQueue to preserve heap property with new higher passenger count!
            waitingDemandQueue.add(batch);
            dbManager.saveDemandBatch(batch);
        } else {
            // Create a new aggregated batch for this corridor
            String newBatchId = "DEM-" + (1100 + batchMap.size() + 1);
            batch = new DemandBatch(
                    newBatchId,
                    pickupStopId,
                    destinationStopId,
                    passengerCount,
                    System.currentTimeMillis(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    "Passenger app surge (" + passengerName + ")",
                    DemandStatus.WAITING
            );
            batch.addRequest(req);
            batchMap.put(newBatchId, batch);
            corridorToBatchMap.put(corridorKey, newBatchId);

            waitingDemandQueue.add(batch);
            dbManager.saveDemandBatch(batch);
        }

        dbManager.saveDemandRequest(req);
        return req;
    }

    // =========================================================================
    // RELIEF BUS ALLOCATION & DIJKSTRA ROUTING
    // =========================================================================

    /**
     * Result of an allocation attempt.
     */
    public record AllocationResult(boolean success, ReliefTrip trip, DemandBatch batch, Bus bus, String message) {}

    /**
     * Allocates the highest-priority waiting demand batch to the optimal reachable available bus.
     */
    public synchronized AllocationResult allocateReliefBus() {
        if (waitingDemandQueue.isEmpty()) {
            return new AllocationResult(false, null, null, null, "No waiting demand requests in queue.");
        }

        // Peek highest priority batch according to DemandPolicy
        DemandBatch batch = waitingDemandQueue.poll();
        if (batch == null || batch.getPassengerCount() <= 0) {
            return new AllocationResult(false, null, null, null, "Top queue item has no waiting passengers.");
        }

        // Find best reachable available bus
        Bus bestBus = null;
        RouteResult bestSegment1 = null;
        RouteResult bestSegment2 = null;
        int minTotalTime = Integer.MAX_VALUE;

        for (Bus bus : busMap.values()) {
            if (!bus.isAvailable()) {
                continue;
            }

            // Segment 1: Bus location -> Pickup Stop
            RouteResult seg1 = router.findShortestRoute(bus.getCurrentLocation(), batch.getPickupStopId(), bus);
            if (!seg1.isReachable()) {
                continue;
            }

            // Segment 2: Pickup Stop -> Destination Stop
            RouteResult seg2 = router.findShortestRoute(batch.getPickupStopId(), batch.getDestinationStopId(), bus);
            if (!seg2.isReachable()) {
                continue;
            }

            int totalTime = seg1.getTotalTravelTime() + seg2.getTotalTravelTime();
            boolean isBetter = false;
            if (totalTime < minTotalTime) {
                isBetter = true;
            } else if (totalTime == minTotalTime && bestBus != null) {
                // Tie-breaker: choose bus with higher capacity to maximize crowd relief
                if (bus.getCapacity() > bestBus.getCapacity()) {
                    isBetter = true;
                }
            }

            if (isBetter) {
                minTotalTime = totalTime;
                bestBus = bus;
                bestSegment1 = seg1;
                bestSegment2 = seg2;
            }
        }

        if (bestBus == null) {
            // Re-insert batch back into PriorityQueue since no bus is currently reachable/available
            waitingDemandQueue.add(batch);
            return new AllocationResult(false, null, batch, null,
                    "No reachable available bus found for route " + batch.getPickupStopId() + " -> " + batch.getDestinationStopId() +
                    " (check bus availability or road restrictions)");
        }

        // Encapsulation: Board passengers up to bus capacity
        int demandCountBefore = batch.getPassengerCount();
        int boarded = bestBus.boardPassengers(demandCountBefore);
        batch.boardPassengers(boarded);

        String tripId = "TRIP-" + tripSequence.incrementAndGet();
        bestBus.setCurrentTripId(tripId);

        ReliefTrip trip = new ReliefTrip(
                tripId,
                batch.getBatchId(),
                bestBus.getBusId(),
                batch.getPickupStopId(),
                batch.getDestinationStopId(),
                boarded,
                bestBus.getCapacity(),
                bestSegment1.getPathStopIds(),
                bestSegment1.getTotalTravelTime(),
                bestSegment2.getPathStopIds(),
                bestSegment2.getTotalTravelTime(),
                TripStatus.IN_PROGRESS
        );

        tripMap.put(tripId, trip);

        // Update database
        dbManager.updateBus(bestBus);
        dbManager.saveDemandBatch(batch);
        dbManager.saveReliefTrip(trip);

        // Retain unserved passengers as pending batch in PriorityQueue
        if (batch.getPassengerCount() > 0) {
            waitingDemandQueue.add(batch); // Re-queued with original arrival order preserved!
        }

        String msg = String.format("Allocated %s [%s] (Cap: %d) to %s. Boarded %d passengers (Remaining: %d). Route time: %d mins.",
                bestBus.getBusType(), bestBus.getBusId(), bestBus.getCapacity(),
                batch.getBatchId(), boarded, batch.getPassengerCount(), trip.getTotalTravelTime());

        return new AllocationResult(true, trip, batch, bestBus, msg);
    }

    /**
     * Completes an active relief trip, releases the bus at the destination stop, and updates status.
     */
    public synchronized boolean completeTrip(String tripId) {
        ReliefTrip trip = tripMap.get(tripId);
        if (trip == null || trip.getStatus() == TripStatus.COMPLETED) {
            return false;
        }

        Bus bus = busMap.get(trip.getBusId());
        if (bus != null) {
            bus.completeTrip(trip.getDestinationStopId());
            dbManager.updateBus(bus);
        }

        trip.setStatus(TripStatus.COMPLETED);
        dbManager.saveReliefTrip(trip);
        return true;
    }

    // =========================================================================
    // DYNAMIC TRAFFIC & INCIDENT MANAGEMENT
    // =========================================================================

    /**
     * Updates road travel time due to traffic conditions and recalculates active paths.
     */
    public synchronized boolean updateRoadTraffic(int roadId, int newTravelTime, String reason) {
        Road road = roadNetwork.getRoad(roadId);
        if (road == null || newTravelTime <= 0) return false;

        int oldTime = road.getTravelTime();
        boolean updated = roadNetwork.updateRoadTravelTime(roadId, newTravelTime);
        if (updated) {
            dbManager.updateRoad(road);
            dbManager.logTrafficChange(roadId, oldTime, newTravelTime, reason != null ? reason : "Supervisor traffic update");

            // Re-evaluate routes for planned or in-progress trips if needed
            for (ReliefTrip trip : tripMap.values()) {
                if (trip.getStatus() == TripStatus.PLANNED) {
                    recalculateTripRoute(trip);
                }
            }
        }
        return updated;
    }

    /**
     * Toggles road open/closed status.
     */
    public synchronized boolean toggleRoadStatus(int roadId, RoadStatus status) {
        Road road = roadNetwork.getRoad(roadId);
        if (road == null) return false;

        boolean updated = roadNetwork.updateRoadStatus(roadId, status);
        if (updated) {
            dbManager.updateRoad(road);
            dbManager.logTrafficChange(roadId, road.getTravelTime(), road.getTravelTime(), "Road status changed to " + status);
        }
        return updated;
    }

    /**
     * Simulates a bus breakdown. If the bus had an assigned trip, the trip is flagged
     * and automatically reassigned to another available bus!
     */
    public synchronized AllocationResult reportBusBreakdown(String busId) {
        Bus brokenBus = busMap.get(busId);
        if (brokenBus == null) {
            return new AllocationResult(false, null, null, null, "Bus " + busId + " not found.");
        }

        String tripId = brokenBus.getCurrentTripId();
        brokenBus.setStatus(BusStatus.BROKEN);
        dbManager.updateBus(brokenBus);

        if (tripId == null) {
            return new AllocationResult(true, null, null, brokenBus, "Bus " + busId + " marked BROKEN. (No active trip was assigned).");
        }

        ReliefTrip affectedTrip = tripMap.get(tripId);
        if (affectedTrip == null || affectedTrip.getStatus() == TripStatus.COMPLETED) {
            brokenBus.releaseBus();
            brokenBus.setStatus(BusStatus.BROKEN);
            dbManager.updateBus(brokenBus);
            return new AllocationResult(true, null, null, brokenBus, "Bus " + busId + " marked BROKEN.");
        }

        affectedTrip.setStatus(TripStatus.REQUIRES_ATTENTION);

        // Find replacement available bus
        Bus replacementBus = null;
        RouteResult repSeg1 = null;
        RouteResult repSeg2 = null;
        int minTime = Integer.MAX_VALUE;

        for (Bus bus : busMap.values()) {
            if (!bus.isAvailable() || bus.getBusId().equals(busId)) {
                continue;
            }
            RouteResult s1 = router.findShortestRoute(bus.getCurrentLocation(), affectedTrip.getPickupStopId(), bus);
            if (!s1.isReachable()) continue;

            RouteResult s2 = router.findShortestRoute(affectedTrip.getPickupStopId(), affectedTrip.getDestinationStopId(), bus);
            if (!s2.isReachable()) continue;

            int total = s1.getTotalTravelTime() + s2.getTotalTravelTime();
            if (total < minTime) {
                minTime = total;
                replacementBus = bus;
                repSeg1 = s1;
                repSeg2 = s2;
            }
        }

        if (replacementBus != null) {
            // Reassign trip
            replacementBus.boardPassengers(affectedTrip.getPassengersAllocated());
            replacementBus.setCurrentTripId(affectedTrip.getTripId());

            affectedTrip.setBusId(replacementBus.getBusId());
            affectedTrip.setSegment1(repSeg1.getPathStopIds(), repSeg1.getTotalTravelTime());
            affectedTrip.setSegment2(repSeg2.getPathStopIds(), repSeg2.getTotalTravelTime());
            affectedTrip.setStatus(TripStatus.IN_PROGRESS);

            dbManager.updateBus(replacementBus);
            dbManager.saveReliefTrip(affectedTrip);

            return new AllocationResult(true, affectedTrip, null, replacementBus,
                    "Bus " + busId + " broke down! Trip " + affectedTrip.getTripId() + " successfully reassigned to " +
                    replacementBus.getBusType() + " [" + replacementBus.getBusId() + "] at " + replacementBus.getCurrentLocation());
        } else {
            // Revert demand back to waiting queue
            DemandBatch batch = batchMap.get(affectedTrip.getBatchId());
            if (batch != null) {
                batch.setPassengerCount(batch.getPassengerCount() + affectedTrip.getPassengersAllocated());
                batch.setStatus(DemandStatus.WAITING);
                waitingDemandQueue.remove(batch);
                waitingDemandQueue.add(batch);
                dbManager.saveDemandBatch(batch);
            }
            affectedTrip.setStatus(TripStatus.CANCELLED);
            dbManager.saveReliefTrip(affectedTrip);

            return new AllocationResult(false, affectedTrip, batch, brokenBus,
                    "Bus " + busId + " broke down. No replacement bus available. " + affectedTrip.getPassengersAllocated() +
                    " passengers returned to waiting queue.");
        }
    }

    /**
     * Repairs and releases a bus back into the available pool.
     */
    public synchronized boolean repairBus(String busId) {
        Bus bus = busMap.get(busId);
        if (bus == null) return false;
        bus.releaseBus();
        dbManager.updateBus(bus);
        return true;
    }

    private void recalculateTripRoute(ReliefTrip trip) {
        Bus bus = busMap.get(trip.getBusId());
        if (bus == null) return;
        RouteResult s1 = router.findShortestRoute(bus.getCurrentLocation(), trip.getPickupStopId(), bus);
        RouteResult s2 = router.findShortestRoute(trip.getPickupStopId(), trip.getDestinationStopId(), bus);
        if (s1.isReachable() && s2.isReachable()) {
            trip.setSegment1(s1.getPathStopIds(), s1.getTotalTravelTime());
            trip.setSegment2(s2.getPathStopIds(), s2.getTotalTravelTime());
            dbManager.saveReliefTrip(trip);
        }
    }

    // =========================================================================
    // GETTERS & READ-ONLY VIEWS FOR UI & CLI
    // =========================================================================

    public List<Stop> getAllStops() {
        return new ArrayList<>(stopMap.values());
    }

    public Stop getStop(String stopId) {
        return stopMap.get(stopId);
    }

    public List<Bus> getAllBuses() {
        return new ArrayList<>(busMap.values());
    }

    public Bus getBus(String busId) {
        return busMap.get(busId);
    }

    public List<Road> getAllRoads() {
        return new ArrayList<>(roadNetwork.getAllRoads());
    }

    public Road getRoad(int roadId) {
        return roadNetwork.getRoad(roadId);
    }

    public List<DemandBatch> getAllDemandBatches() {
        return new ArrayList<>(batchMap.values());
    }

    public List<DemandBatch> getRankedWaitingBatches() {
        // Return a sorted copy based on current DemandPolicy
        List<DemandBatch> list = new ArrayList<>();
        for (DemandBatch b : batchMap.values()) {
            if ((b.getStatus() == DemandStatus.WAITING || b.getStatus() == DemandStatus.PARTIALLY_ALLOCATED) && b.getPassengerCount() > 0) {
                list.add(b);
            }
        }
        list.sort(demandPolicy.getComparator());
        return list;
    }

    public List<ReliefTrip> getAllTrips() {
        List<ReliefTrip> list = new ArrayList<>(tripMap.values());
        list.sort((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));
        return list;
    }

    public ReliefTrip getTrip(String tripId) {
        if (tripId == null) return null;
        return tripMap.get(tripId);
    }

    public DijkstraRouter getRouter() {
        return router;
    }

    public WeightedGraph getRoadNetwork() {
        return roadNetwork;
    }

    public DemandPolicy getDemandPolicy() {
        return demandPolicy;
    }

    public synchronized void setDemandPolicy(DemandPolicy policy) {
        if (policy == null) return;
        this.demandPolicy = policy;
        // Rebuild priority queue with new policy comparator
        List<DemandBatch> active = new ArrayList<>(this.waitingDemandQueue);
        this.waitingDemandQueue.clear();
        PriorityQueue<DemandBatch> newQueue = new PriorityQueue<>(policy.getComparator());
        newQueue.addAll(active);
    }

    public User authenticate(String username, String password) {
        User u = dbManager.loadUser(username);
        if (u != null && u.checkPassword(password)) {
            return u;
        }
        return null;
    }

    // Metric Calculations for SDG 9, 11, 13
    public record TransitMetrics(
            int totalPassengersWaiting,
            int totalPassengersServed,
            int activeReliefTrips,
            int availableBuses,
            int totalFleetSize,
            double fleetUtilizationPercent,
            double co2EmissionsSavedKg,
            double averageWaitTimeMinutes
    ) {}

    public TransitMetrics calculateMetrics() {
        int waiting = 0;
        int served = 0;
        for (DemandBatch b : batchMap.values()) {
            waiting += b.getPassengerCount();
            served += (b.getOriginalCount() - b.getPassengerCount());
        }

        int activeTrips = 0;
        for (ReliefTrip t : tripMap.values()) {
            if (t.getStatus() == TripStatus.IN_PROGRESS || t.getStatus() == TripStatus.PLANNED) {
                activeTrips++;
            }
        }

        int avail = 0;
        for (Bus b : busMap.values()) {
            if (b.isAvailable()) avail++;
        }

        int totalFleet = busMap.size();
        double utilization = totalFleet > 0 ? ((double) (totalFleet - avail) / totalFleet) * 100.0 : 0.0;

        // SDG 13 Metric: Assuming average relief trip replaces ~25 private two-wheelers/cars
        // Saving approx 0.12 kg CO2 per passenger trip
        double co2Saved = served * 0.45;

        double avgWaitTime = waitingDemandQueue.isEmpty() ? 5.0 : Math.min(30.0, 5.0 + (waitingDemandQueue.size() * 2.5));

        return new TransitMetrics(waiting, served, activeTrips, avail, totalFleet, utilization, co2Saved, avgWaitTime);
    }
}
