package org.vetrinagar.policy;

import org.vetrinagar.model.DemandBatch;

import java.util.Comparator;

/**
 * Standard demand ranking policy:
 * 1. Passenger Count (DESC) - highest crowd surge receives relief first.
 * 2. Arrival Order / Timestamp (ASC) - first-come, first-served when passenger counts are equal.
 * 3. Batch ID (ASC) - deterministic tie-breaker to prevent unstable queue order.
 */
public class DefaultDemandPolicy implements DemandPolicy {

    private final Comparator<DemandBatch> comparator;

    public DefaultDemandPolicy() {
        this.comparator = (b1, b2) -> {
            if (b1 == b2) return 0;
            if (b1 == null) return 1;
            if (b2 == null) return -1;

            // 1. Highest passenger count first
            int countComparison = Integer.compare(b2.getPassengerCount(), b1.getPassengerCount());
            if (countComparison != 0) {
                return countComparison;
            }

            // 2. Earliest arrival order first
            int timeComparison = Long.compare(b1.getArrivalTimestamp(), b2.getArrivalTimestamp());
            if (timeComparison != 0) {
                return timeComparison;
            }

            // 3. Deterministic tie-breaker
            return b1.getBatchId().compareTo(b2.getBatchId());
        };
    }

    @Override
    public Comparator<DemandBatch> getComparator() {
        return this.comparator;
    }

    @Override
    public String getPolicyName() {
        return "Standard Peak-Hour Relief (Count DESC, Arrival ASC)";
    }
}
