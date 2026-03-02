package com.project.query;

import com.project.core.Graph;
import com.project.core.Node;
import com.project.storage.GraphStorageEngine;
import com.project.traversal.GraphTraverser;
import com.project.traversal.QueryPlan;
import com.project.traversal.filters.*;

import java.io.IOException;

public class ExecutionEngine {

    private final Graph graph;
    private final GraphTraverser traverser;
    private final GraphStorageEngine graphStorageEngine;

    public ExecutionEngine(Graph graph, GraphTraverser traverser, GraphStorageEngine graphStorageEngine) {
        this.graph = graph;
        this.traverser = traverser;
        this.graphStorageEngine = graphStorageEngine;
    }

    public Object execute(QueryPlan plan) throws IOException, InterruptedException {

        switch(plan.operation) {
            case FIND -> {
                TraversalFilter filter = buildFilter(plan);
                switch(plan.strategy) {
                    case BFS -> {
                        return traverser.bfs(plan.startId, plan.depth, filter);
                    }
                    case NEIGHBOURS -> {
                        return traverser.neighbors(plan.startId, filter);
                    }
                    case SHORTEST_PATH -> {
                        return traverser.shortestPath(plan.startId, plan.endId, filter);
                    }
                    default -> {
                        throw new IllegalStateException("Unexpected value: " + plan.strategy);
                    }
                }
            }
            case CREATE -> {
                switch(plan.targetType) {
                    case "NODE" -> {
                        System.out.println("CREATING A NODE!");
                        Node node = new Node(plan.label, plan.props);
                        graphStorageEngine.addNode(node);
                        return "Added node: [" + node.getLabel() + "]";
                    }
                    default -> {
                        throw new IllegalStateException("Unexpected value: " + plan.targetType);
                    }
                }
            }
            default -> {
                throw new IllegalStateException();
            }
        }
    }

    private TraversalFilter buildFilter(QueryPlan plan) {
        TraversalFilter filter = AllowAllFilter.INSTANCE;

        if (plan.nodeFilter != null) {
            filter = new NodeTypeFilter(plan.nodeFilter, graph);
        }

        if (plan.edgeFilter != null) {
            TraversalFilter edgeFilter = new EdgeTypeFilter(plan.edgeFilter, graph);

            filter = (filter == AllowAllFilter.INSTANCE) ? edgeFilter : new CombinedFilter(filter, edgeFilter);
        }

        return filter;
    }
}
