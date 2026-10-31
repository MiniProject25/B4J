package com.project.traversal.filters;

import com.project.core.Edge;
import com.project.core.EdgeRecord;
import com.project.core.Graph;

import java.util.Objects;
import java.util.UUID;

public class EdgeTypeFilter implements TraversalFilter {
    private final Graph graph;
    private final String allowedType;

    public EdgeTypeFilter(String allowedType, Graph graph) {
        this.allowedType = allowedType;
        this.graph = graph;
    }

    @Override
    public boolean allowNode(UUID node) {
        return true;
    }

    @Override
    public boolean allowEdge(UUID edge) {
        if (edge == null) return false;
        EdgeRecord er = graph.getEdges().get(edge);
        if (er == null || er.getEdge() == null) return false;
        Edge e = er.getEdge();
        return Objects.equals(allowedType, e.getType());
    }
}
