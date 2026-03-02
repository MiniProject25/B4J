package com.project.traversal;

import java.util.UUID;

public class TraversalNode {
    public final UUID nodeId;
    public final int depth;
    public final UUID parentId;

    public TraversalNode(UUID nodeId, int depth, UUID parentId) {
        this.nodeId = nodeId;
        this.depth = depth;
        this.parentId = parentId;
    }
}
