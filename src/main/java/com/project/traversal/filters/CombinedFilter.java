package com.project.traversal.filters;

import java.util.UUID;

public class CombinedFilter implements  TraversalFilter {

    private final TraversalFilter filter1;
    private final TraversalFilter filter2;

    public CombinedFilter(TraversalFilter filter1, TraversalFilter filter2) {
        this.filter1 = filter1;
        this.filter2 = filter2;
    }

    public boolean allowNode(UUID nodeId) {
        return (filter1.allowNode(nodeId) && filter2.allowNode(nodeId));
    }

    public boolean allowEdge(UUID edgeId) {
        return (filter1.allowEdge(edgeId) && filter2.allowEdge(edgeId));
    }
}
