package com.project.traversal.filters;

import com.project.core.Graph;
import com.project.core.Node;
import com.project.core.NodeRecord;

import java.util.Objects;
import java.util.UUID;

public class NodeTypeFilter implements TraversalFilter {

    private final Graph graph;
    private final String allowedType;

    public NodeTypeFilter(String allowedType, Graph graph) {
        this.allowedType = allowedType;
        this.graph = graph;
    }

    @Override
    public boolean allowNode(UUID node) {
        if (node == null) return false;
        NodeRecord nr = graph.getNodes().get(node);
        if (nr == null || nr.getNode() == null) return false;
        Node n = nr.getNode();
        return Objects.equals(n.getLabel(), allowedType);
    }

    @Override
    public boolean allowEdge(UUID edge) {
        return true;
    }
}
