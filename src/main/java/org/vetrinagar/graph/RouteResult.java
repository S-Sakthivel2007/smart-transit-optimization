package org.vetrinagar.graph;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the outcome of a Dijkstra shortest-path query.
 */
public class RouteResult {
    private final boolean reachable;
    private final List<String> pathStopIds;
    private final int totalTravelTime; // in minutes
    private final String originStopId;
    private final String destinationStopId;
    private final String errorMessage;

    public static RouteResult unreachable(String origin, String destination, String reason) {
        return new RouteResult(false, Collections.emptyList(), Integer.MAX_VALUE, origin, destination, reason);
    }

    public static RouteResult of(String origin, String destination, List<String> pathStopIds, int totalTravelTime) {
        return new RouteResult(true, pathStopIds, totalTravelTime, origin, destination, null);
    }

    private RouteResult(boolean reachable, List<String> pathStopIds, int totalTravelTime,
                        String originStopId, String destinationStopId, String errorMessage) {
        this.reachable = reachable;
        this.pathStopIds = pathStopIds != null ? List.copyOf(pathStopIds) : Collections.emptyList();
        this.totalTravelTime = totalTravelTime;
        this.originStopId = originStopId;
        this.destinationStopId = destinationStopId;
        this.errorMessage = errorMessage;
    }

    public boolean isReachable() {
        return reachable;
    }

    public List<String> getPathStopIds() {
        return pathStopIds;
    }

    public int getTotalTravelTime() {
        return totalTravelTime;
    }

    public String getOriginStopId() {
        return originStopId;
    }

    public String getDestinationStopId() {
        return destinationStopId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getFormattedPath() {
        if (!reachable) {
            return "UNREACHABLE: " + (errorMessage != null ? errorMessage : "No valid open path");
        }
        return String.join(" -> ", pathStopIds) + " (" + totalTravelTime + " mins)";
    }

    @Override
    public String toString() {
        return getFormattedPath();
    }
}
