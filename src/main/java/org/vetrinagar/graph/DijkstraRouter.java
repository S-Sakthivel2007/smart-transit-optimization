package org.vetrinagar.graph;

import org.vetrinagar.model.Bus;
import org.vetrinagar.model.Road;

import java.util.*;

/**
 * Dijkstra's shortest-path algorithm using a min-heap PriorityQueue.
 * Computes minimum-time relief transit paths while enforcing road status
 * and bus vehicle restrictions polymorphically.
 */
public class DijkstraRouter {
    private final WeightedGraph graph;

    public DijkstraRouter(WeightedGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph cannot be null");
    }

    /**
     * Internal node wrapper for the min-heap PriorityQueue.
     */
    private static class NodeDistance implements Comparable<NodeDistance> {
        final String stopId;
        final int distance; // cumulative travel time in minutes

        NodeDistance(String stopId, int distance) {
            this.stopId = stopId;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    /**
     * Computes the shortest travel-time path between origin and destination for a given bus.
     *
     * @param originStopId      Starting stop ID
     * @param destinationStopId Ending stop ID
     * @param bus               The bus executing the trip (may be null for generic query)
     * @return RouteResult containing path, total travel time, or unreachable status
     */
    public RouteResult findShortestRoute(String originStopId, String destinationStopId, Bus bus) {
        if (originStopId == null || destinationStopId == null) {
            return RouteResult.unreachable(originStopId, destinationStopId, "Origin or destination stop ID is null");
        }

        if (!graph.hasStop(originStopId)) {
            return RouteResult.unreachable(originStopId, destinationStopId, "Origin stop " + originStopId + " does not exist in network");
        }

        if (!graph.hasStop(destinationStopId)) {
            return RouteResult.unreachable(originStopId, destinationStopId, "Destination stop " + destinationStopId + " does not exist in network");
        }

        // Trivial case: origin is destination
        if (originStopId.equals(destinationStopId)) {
            return RouteResult.of(originStopId, destinationStopId, Collections.singletonList(originStopId), 0);
        }

        Map<String, Integer> minTimes = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<NodeDistance> minHeap = new PriorityQueue<>();

        // Initialize distances
        for (var stop : graph.getAllStops()) {
            minTimes.put(stop.getStopId(), Integer.MAX_VALUE);
        }

        minTimes.put(originStopId, 0);
        minHeap.add(new NodeDistance(originStopId, 0));

        Set<String> visited = new HashSet<>();

        while (!minHeap.isEmpty()) {
            NodeDistance current = minHeap.poll();
            String currentStopId = current.stopId;

            if (visited.contains(currentStopId)) {
                continue;
            }
            visited.add(currentStopId);

            // Early exit if destination reached
            if (currentStopId.equals(destinationStopId)) {
                break;
            }

            if (current.distance > minTimes.getOrDefault(currentStopId, Integer.MAX_VALUE)) {
                continue;
            }

            for (Road road : graph.getOutgoingRoads(currentStopId)) {
                // Check road status
                if (!road.isOpen()) {
                    continue;
                }

                // Check vehicle road restriction polymorphically
                if (bus != null && !bus.canUseRoad(road)) {
                    continue;
                }

                String neighborId = road.getToStopId();
                if (visited.contains(neighborId)) {
                    continue;
                }

                int edgeWeight = road.getTravelTime();
                int newDist = current.distance + edgeWeight;

                if (newDist < minTimes.getOrDefault(neighborId, Integer.MAX_VALUE)) {
                    minTimes.put(neighborId, newDist);
                    predecessors.put(neighborId, currentStopId);
                    minHeap.add(new NodeDistance(neighborId, newDist));
                }
            }
        }

        int totalTime = minTimes.getOrDefault(destinationStopId, Integer.MAX_VALUE);
        if (totalTime == Integer.MAX_VALUE) {
            String reason = bus != null
                    ? "No reachable open route for " + bus.getBusType() + " [" + bus.getBusId() + "] (check road closures or bridge restrictions)"
                    : "No open route connects " + originStopId + " and " + destinationStopId;
            return RouteResult.unreachable(originStopId, destinationStopId, reason);
        }

        // Reconstruct path
        LinkedList<String> path = new LinkedList<>();
        String curr = destinationStopId;
        while (curr != null) {
            path.addFirst(curr);
            curr = predecessors.get(curr);
        }

        return RouteResult.of(originStopId, destinationStopId, path, totalTime);
    }
}
