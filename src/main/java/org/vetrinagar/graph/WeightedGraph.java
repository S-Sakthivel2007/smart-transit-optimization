package org.vetrinagar.graph;

import org.vetrinagar.model.Road;
import org.vetrinagar.model.RoadStatus;
import org.vetrinagar.model.RoadType;
import org.vetrinagar.model.Stop;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Weighted graph representing Vetrinagar's transit network.
 * Vertices are Stops and Depot; Edges are Roads with positive travel times.
 */
public class WeightedGraph {
    private final Map<String, Stop> stops;
    private final Map<Integer, Road> roadsById;
    // Adjacency list: stopId -> List of outgoing Road objects
    private final Map<String, List<Road>> adjacencyList;

    public WeightedGraph() {
        this.stops = new ConcurrentHashMap<>();
        this.roadsById = new ConcurrentHashMap<>();
        this.adjacencyList = new ConcurrentHashMap<>();
    }

    public synchronized void addStop(Stop stop) {
        if (stop == null) return;
        stops.put(stop.getStopId(), stop);
        adjacencyList.computeIfAbsent(stop.getStopId(), k -> new ArrayList<>());
    }

    public synchronized void addRoad(Road road) {
        if (road == null) return;
        roadsById.put(road.getRoadId(), road);

        adjacencyList.computeIfAbsent(road.getFromStopId(), k -> new ArrayList<>()).add(road);

        // If road is two-way, also register a reverse link for graph traversal
        if (road.isTwoWay()) {
            Road reverseRoad = new Road(
                    -road.getRoadId(), // negative ID denotes reverse traversal of the same road
                    road.getToStopId(),
                    road.getFromStopId(),
                    road.getTravelTime(),
                    road.getStatus(),
                    road.getRoadType(),
                    road.getWeightLimit()
            );
            adjacencyList.computeIfAbsent(road.getToStopId(), k -> new ArrayList<>()).add(reverseRoad);
        }
    }

    public List<Road> getOutgoingRoads(String stopId) {
        return adjacencyList.getOrDefault(stopId, Collections.emptyList());
    }

    public Stop getStop(String stopId) {
        return stops.get(stopId);
    }

    public Collection<Stop> getAllStops() {
        return Collections.unmodifiableCollection(stops.values());
    }

    public Road getRoad(int roadId) {
        return roadsById.get(roadId);
    }

    public Collection<Road> getAllRoads() {
        return Collections.unmodifiableCollection(roadsById.values());
    }

    /**
     * Updates travel time on a road segment (traffic congestion / clearing).
     */
    public synchronized boolean updateRoadTravelTime(int roadId, int newTravelTime) {
        Road road = roadsById.get(roadId);
        if (road == null || newTravelTime <= 0) return false;
        road.setTravelTime(newTravelTime);

        // Update reverse road if two-way
        if (road.isTwoWay()) {
            List<Road> reverseList = adjacencyList.get(road.getToStopId());
            if (reverseList != null) {
                for (Road r : reverseList) {
                    if (r.getRoadId() == -roadId) {
                        r.setTravelTime(newTravelTime);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Updates status on a road segment (open / closed).
     */
    public synchronized boolean updateRoadStatus(int roadId, RoadStatus status) {
        Road road = roadsById.get(roadId);
        if (road == null) return false;
        road.setStatus(status);

        if (road.isTwoWay()) {
            List<Road> reverseList = adjacencyList.get(road.getToStopId());
            if (reverseList != null) {
                for (Road r : reverseList) {
                    if (r.getRoadId() == -roadId) {
                        r.setStatus(status);
                    }
                }
            }
        }
        return true;
    }

    public boolean hasStop(String stopId) {
        return stops.containsKey(stopId);
    }
}
