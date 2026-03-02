package com.project.traversal;

import java.util.*;

public class TraversalResult {
    private final Map<UUID, TraversalNode> visited;

    public TraversalResult(Map<UUID,  TraversalNode> visited) {
        this.visited = visited;
    }

    // get the visited list
    public Collection<TraversalNode> getNodes() {
        return visited.values();
    }

    public TraversalNode get(UUID id) {
        return visited.get(id);
    }
}
