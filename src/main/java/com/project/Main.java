package com.project;

import com.project.config.db.Config;
import com.project.core.Edge;
import com.project.core.EdgeRecord;
import com.project.core.Graph;
import com.project.core.Node;
import com.project.core.NodeRecord;
import com.project.query.BotaParser;
import com.project.query.ExecutionEngine;
import com.project.server.BootManager;
import com.project.storage.GraphStorageEngine;
import com.project.storage.checkpoint.CheckpointManager;
import com.project.storage.checkpoint.SnapshotService;
import com.project.storage.io.EdgeRecordWriter;
import com.project.storage.io.NodeRecordWriter;
import com.project.storage.loader.GraphLoader;
import com.project.storage.typeregistry.EdgeTypeRegistry;
import com.project.storage.typeregistry.NodeTypeRegistry;
import com.project.storage.wal.WALWriter;
import com.project.traversal.GraphTraverser;
import com.project.traversal.QueryPlan;
import com.project.traversal.TraversalNode;
import com.project.traversal.TraversalResult;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        Config config;
        try {
            config = Config.init(args);
        } catch (Exception e) {
            System.err.println("Failed to initialize database configuration: " + e.getMessage());
            return;
        }

        // Shared in-memory graph
        Graph graph = new Graph();

        NodeTypeRegistry nodeTypes = null;
        EdgeTypeRegistry edgeTypes = null;
        NodeRecordWriter nodeRecordWriter = null;
        EdgeRecordWriter edgeRecordWriter = null;
        WALWriter walWriter = null;
        CheckpointManager checkpointManager = null;
        GraphStorageEngine storageEngine = null;

        try {
            // Load registries
            nodeTypes = new NodeTypeRegistry(config.getFilePath());
            edgeTypes = new EdgeTypeRegistry(config.getFilePath());
            nodeTypes.load();
            edgeTypes.load();

            nodeRecordWriter = new NodeRecordWriter(config.nodesPath());
            edgeRecordWriter = new EdgeRecordWriter(config.edgesPath());
            walWriter = new WALWriter(config.walPath());

            GraphLoader loader = new GraphLoader(nodeRecordWriter, edgeRecordWriter);
            SnapshotService snapshotService = new SnapshotService(config.nodesPath(), config.edgesPath(), nodeRecordWriter, edgeRecordWriter);
            checkpointManager = new CheckpointManager(snapshotService, walWriter);

            storageEngine = new GraphStorageEngine(graph, nodeRecordWriter, edgeRecordWriter, walWriter, loader, checkpointManager, snapshotService);
            BootManager bootManager = new BootManager(storageEngine, loader, graph, config);

            GraphTraverser graphTraverser = new GraphTraverser(graph);
            BotaParser botaParser = new BotaParser();
            ExecutionEngine executionEngine = new ExecutionEngine(graph, graphTraverser, storageEngine);

            // Boot: recover from WAL or load snapshot into the shared graph instance
            bootManager.boot(config.getDbName());

            System.out.println("\nB4J Interactive Graph Database Ready!");
            System.out.println("Type 'HELP' for query reference or 'END' to exit.\n");

            Scanner scanner = new Scanner(System.in);

            while (scanner.hasNextLine()) {
                System.out.print("b4j> ");
                String command = scanner.nextLine().trim();

                if (command.isEmpty()) {
                    continue;
                }

                if (command.equalsIgnoreCase("END") || command.equalsIgnoreCase("EXIT") || command.equalsIgnoreCase("QUIT")) {
                    break;
                }

                try {
                    QueryPlan plan = botaParser.parseQuery(command);
                    Object result = executionEngine.execute(plan);
                    printResult(result, plan, graphTraverser, graph);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Fatal database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("\nShutting down Bota4J...");
            if (checkpointManager != null && graph != null) {
                try {
                    System.out.println("Saving checkpoint snapshot...");
                    checkpointManager.createCheckpoint(graph.getNodes(), graph.getEdges());
                } catch (Exception e) {
                    System.err.println("Failed to create shutdown checkpoint: " + e.getMessage());
                }
            }

            if (storageEngine != null) {
                storageEngine.shutdownExecutor();
            }

            try {
                if (walWriter != null) walWriter.close();
                if (nodeRecordWriter != null) nodeRecordWriter.close();
                if (edgeRecordWriter != null) edgeRecordWriter.close();
                if (nodeTypes != null) nodeTypes.write();
                if (edgeTypes != null) edgeTypes.write();
            } catch (Exception e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }

            System.out.println("Shutdown complete. Goodbye!");
        }
    }

    private static String fmtNode(UUID id, Graph g) {
        if (id == null) return "[null]";
        NodeRecord nr = g.getNodes().get(id);
        if (nr == null || nr.getNode() == null) {
            return "[" + id + "]";
        }
        Node node = nr.getNode();
        String idShort = id.toString().length() >= 8 ? id.toString().substring(0, 8) : id.toString();
        if (node.getProperties() != null && !node.getProperties().isEmpty()) {
            return "[" + node.getLabel() + ": " + idShort + " " + node.getProperties() + "]";
        }
        return "[" + node.getLabel() + ": " + idShort + "]";
    }

    private static String fmtEdge(UUID edgeId, Graph g) {
        if (edgeId == null) return "---->";
        EdgeRecord er = g.getEdges().get(edgeId);
        if (er == null || er.getEdge() == null) {
            return "---->";
        }
        Edge edge = er.getEdge();
        return "--" + edge.getType() + "-->";
    }

    private static void printPath(List<UUID> path, Graph g) {
        if (path == null || path.isEmpty()) {
            System.out.println("No path found");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            sb.append(fmtNode(path.get(i), g));

            if (i < path.size() - 1) {
                UUID from = path.get(i);
                UUID to = path.get(i + 1);
                String edgeFmt = " --> ";

                for (UUID edge : g.getOutgoingAdjacencyList().getOrDefault(from, List.of())) {
                    EdgeRecord er = g.getEdges().get(edge);
                    if (er != null && er.getEdge() != null && er.getEdge().getTarget().equals(to)) {
                        edgeFmt = " " + fmtEdge(edge, g) + " ";
                        break;
                    }
                }
                sb.append(edgeFmt);
            }
        }

        System.out.println(sb.toString());
    }

    private static void printNeighbors(List<UUID> neighbors, Graph g) {
        if (neighbors == null || neighbors.isEmpty()) {
            System.out.println("No neighbours found");
            return;
        }
        System.out.println("Neighbours (" + neighbors.size() + "):");
        for (UUID neighborId : neighbors) {
            System.out.println("  " + fmtNode(neighborId, g));
        }
    }

    private static void printBfs(TraversalResult r, Graph g) {
        if (r == null || r.getNodes().isEmpty()) {
            System.out.println("No nodes reached");
            return;
        }
        System.out.println("Visited Nodes (" + r.getNodes().size() + "):");
        for (TraversalNode n : r.getNodes()) {
            System.out.println("  (depth " + n.depth + ") " + fmtNode(n.nodeId, g));
        }
    }

    @SuppressWarnings("unchecked")
    private static void printResult(Object result, QueryPlan plan, GraphTraverser traverser, Graph g) {
        if (result == null) {
            System.out.println("No Result");
            return;
        }

        switch (plan.operation) {
            case CREATE, DELETE, SHOW, HELP -> {
                System.out.println(result);
            }
            case FIND -> {
                switch (plan.strategy) {
                    case BFS -> {
                        printBfs((TraversalResult) result, g);
                    }
                    case NEIGHBOURS -> {
                        printNeighbors((List<UUID>) result, g);
                    }
                    case SHORTEST_PATH -> {
                        TraversalResult tr = (TraversalResult) result;
                        List<UUID> path = traverser.reconstructPath(tr, plan.startId, plan.endId);
                        printPath(path, g);
                    }
                }
            }
        }
    }
}
