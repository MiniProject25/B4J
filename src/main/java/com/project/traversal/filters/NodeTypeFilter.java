package com.project.traversal.filters;

import com.project.core.Graph;
import com.project.core.Node;

import java.util.Objects;
import java.util.UUID;

public class NodeTypeFilter implements TraversalFilter{

    private final Graph graph;
    private final String allowedType;

    public NodeTypeFilter(String allowedType, Graph graph) {
        this.allowedType = allowedType;
        this.graph = graph;
    }

    @Override
    public boolean allowNode(UUID node) {
        Node n = graph.getNodes().get(node).getNode();
        return (n != null && Objects.equals(n.getLabel(), allowedType));
    }

    @Override
    public boolean allowEdge(UUID edge) {
        return true;
    }
}
