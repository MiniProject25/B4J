package com.project.traversal.filters;

import java.util.UUID;

public class AllowAllFilter implements TraversalFilter {

    public static final AllowAllFilter INSTANCE = new AllowAllFilter();

    private AllowAllFilter() {}

    @Override
    public boolean allowNode(UUID node) {
        return true;
    }

    @Override
    public boolean allowEdge(UUID edge) {
        return true;
    }
}
