package com.project.traversal.filters;

import com.project.core.Edge;
import com.project.core.Graph;

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
        Edge e = graph.getEdges().get(edge).getEdge();
        return (e != null && allowedType.equals(e.getType()));
    }
}
