package com.project.query;

import com.project.core.Edge;
import com.project.core.EdgeRecord;
import com.project.core.Graph;
import com.project.core.Node;
import com.project.core.NodeRecord;
import com.project.storage.GraphStorageEngine;
import com.project.traversal.GraphTraverser;
import com.project.traversal.QueryPlan;
import com.project.traversal.filters.*;

import java.io.IOException;
import java.util.UUID;

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
        if (plan == null || plan.operation == null) {
            throw new IllegalArgumentException("Invalid query plan");
        }

        switch (plan.operation) {
            case CREATE -> {
                switch (plan.targetType.toUpperCase()) {
                    case "NODE" -> {
                        Node node = new Node(plan.label, plan.props);
                        graphStorageEngine.addNode(node);
                        return "Created Node [" + node.getLabel() + "] with ID: " + node.getId();
                    }
                    case "EDGE" -> {
                        if (!graph.getNodes().containsKey(plan.fromID)) {
                            throw new IllegalArgumentException("Source node does not exist: " + plan.fromID);
                        }
                        if (!graph.getNodes().containsKey(plan.toID)) {
                            throw new IllegalArgumentException("Target node does not exist: " + plan.toID);
                        }
                        UUID edgeId = UUID.randomUUID();
                        Edge edge = new Edge(edgeId, plan.label, plan.fromID, plan.toID, plan.props);
                        graphStorageEngine.addEdge(edge);
                        return "Created Edge --[" + edge.getType() + "]--> with ID: " + edge.getId() +
                               " (from: " + edge.getSource() + " to: " + edge.getTarget() + ")";
                    }
                    default -> throw new IllegalStateException("Unexpected CREATE target: " + plan.targetType);
                }
            }

            case FIND -> {
                TraversalFilter filter = buildFilter(plan);
                switch (plan.strategy) {
                    case BFS -> {
                        return traverser.bfs(plan.startId, plan.depth, filter);
                    }
                    case NEIGHBOURS -> {
                        return traverser.neighbors(plan.startId, filter);
                    }
                    case SHORTEST_PATH -> {
                        return traverser.shortestPath(plan.startId, plan.endId, filter);
                    }
                    default -> throw new IllegalStateException("Unexpected traversal strategy: " + plan.strategy);
                }
            }

            case DELETE -> {
                switch (plan.deleteTarget.toUpperCase()) {
                    case "NODE" -> {
                        if (!graph.getNodes().containsKey(plan.targetId)) {
                            throw new IllegalArgumentException("Node not found: " + plan.targetId);
                        }
                        graphStorageEngine.deleteNode(plan.targetId);
                        return "Deleted Node: " + plan.targetId;
                    }
                    case "EDGE" -> {
                        if (!graph.getEdges().containsKey(plan.targetId)) {
                            throw new IllegalArgumentException("Edge not found: " + plan.targetId);
                        }
                        graphStorageEngine.deleteEdge(plan.targetId);
                        return "Deleted Edge: " + plan.targetId;
                    }
                    default -> throw new IllegalArgumentException("Unexpected DELETE target: " + plan.deleteTarget);
                }
            }

            case SHOW -> {
                switch (plan.showTarget.toUpperCase()) {
                    case "GRAPH" -> {
                        return graph.toString();
                    }
                    case "NODES" -> {
                        if (graph.getNodes().isEmpty()) {
                            return "No nodes in database.";
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("Nodes (").append(graph.getNodes().size()).append("):\n");
                        for (NodeRecord nr : graph.getNodes().values()) {
                            if (nr != null && nr.getNode() != null) {
                                sb.append("  ").append(nr.getNode()).append("\n");
                            }
                        }
                        return sb.toString().trim();
                    }
                    case "EDGES" -> {
                        if (graph.getEdges().isEmpty()) {
                            return "No edges in database.";
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("Edges (").append(graph.getEdges().size()).append("):\n");
                        for (EdgeRecord er : graph.getEdges().values()) {
                            if (er != null && er.getEdge() != null) {
                                sb.append("  ").append(er.getEdge()).append("\n");
                            }
                        }
                        return sb.toString().trim();
                    }
                    default -> throw new IllegalArgumentException("Unknown SHOW target: '" + plan.showTarget + "'. Expected GRAPH, NODES, or EDGES.");
                }
            }

            case HELP -> {
                return """
                    ================== BOTA4J QUERY REFERENCE ==================
                    1. Create Node:
                       CREATE NODE <Label> [{key: value, ...}]
                       Example: CREATE NODE Person {"name": "Alice", "age": 30}
                    
                    2. Create Edge:
                       CREATE EDGE <Type> FROM <sourceUUID> TO <targetUUID> [{props}]
                       Example: CREATE EDGE KNOWS FROM <uuid1> TO <uuid2> {"since": 2021}
                    
                    3. Find Nodes (BFS):
                       FIND NODES FROM <sourceUUID> [DEPTH <n>] [WHERE [NODE_LABEL <label>] [AND] [EDGE_LABEL <label>]]
                       Example: FIND NODES FROM <uuid> DEPTH 2 WHERE NODE_LABEL Person
                    
                    4. Find Neighbours:
                       FIND NEIGHBOURS OF <sourceUUID> [WHERE [NODE_LABEL <label>] [AND] [EDGE_LABEL <label>]]
                       Example: FIND NEIGHBOURS OF <uuid>
                    
                    5. Find Shortest Path:
                       FIND PATH FROM <sourceUUID> TO <targetUUID> [WHERE [EDGE_LABEL <label>]]
                       Example: FIND PATH FROM <uuid1> TO <uuid2>
                    
                    6. Inspection:
                       SHOW GRAPH    (Summary of nodes, edges and adjacency lists)
                       SHOW NODES    (List all nodes)
                       SHOW EDGES    (List all edges)
                    
                    7. Deletion:
                       DELETE NODE <UUID>
                       DELETE EDGE <UUID>
                    
                    8. Exit:
                       END / EXIT / QUIT
                    ============================================================
                    """.trim();
            }

            default -> throw new IllegalStateException("Unhandled operation: " + plan.operation);
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
