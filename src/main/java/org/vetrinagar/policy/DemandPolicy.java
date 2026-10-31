package org.vetrinagar.policy;

import org.vetrinagar.model.DemandBatch;

import java.util.Comparator;

/**
 * Abstraction for demand ranking strategies.
 * Allows supervisor to swap or adjust ranking rules (e.g. VIP, Medical priority, or Standard).
 */
public interface DemandPolicy {
    /**
     * Returns a comparator for ordering DemandBatch objects in the PriorityQueue.
     */
    Comparator<DemandBatch> getComparator();

    /**
     * Policy name / description for auditing and display.
     */
    String getPolicyName();
}
