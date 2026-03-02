package com.project.traversal.filters;

import java.util.UUID;

public interface TraversalFilter {

    public boolean allowNode(UUID node);
    public boolean allowEdge(UUID edge);
//    public boolean allowStep(UUID from, UUID edge, UUID to);
}
