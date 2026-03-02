package com.project;

import com.project.config.db.Config;
import com.project.core.Edge;
import com.project.core.Graph;
import com.project.core.Node;
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
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws Exception {

        // 1. Config DB
        Config config = Config.init();

        // 2. Create components using config path
        Graph graph = new Graph();

        // 3. Load the labels/types
        NodeTypeRegistry nodeTypes = new NodeTypeRegistry(config.getFilePath());
        EdgeTypeRegistry edgeTypes = new EdgeTypeRegistry(config.getFilePath());

        nodeTypes.load();
        edgeTypes.load();
        NodeRecordWriter nodeRecordWriter = new NodeRecordWriter(config.nodesPath());
        EdgeRecordWriter edgeRecordWriter = new EdgeRecordWriter(config.edgesPath());
        WALWriter walWriter = new WALWriter(config.walPath());

        GraphLoader loader = new GraphLoader(nodeRecordWriter, edgeRecordWriter);
        SnapshotService snapshotService = new SnapshotService(config.nodesPath(), config.edgesPath(), nodeRecordWriter, edgeRecordWriter);
        CheckpointManager checkpointManager = new CheckpointManager(snapshotService, walWriter);

        GraphStorageEngine storageEngine = new GraphStorageEngine(graph, nodeRecordWriter, edgeRecordWriter, walWriter, loader, checkpointManager);
        BootManager bootManager = new BootManager(storageEngine, loader, graph);

        GraphTraverser graphTraverser = new GraphTraverser(graph);
        BotaParser botaParser = new BotaParser();
        ExecutionEngine executionEngine = new ExecutionEngine(graph, graphTraverser, storageEngine);

        Scanner scanner = new Scanner(System.in);

        try {
            // 4. Boot
            graph = bootManager.boot(config.getDbName());
            System.out.println(graph);

            // --------------------- DUMMY DATA --------------------- //

            // 5. CLI
            while(true) {
                System.out.print("b4j> ");
                String command = scanner.nextLine().trim();

                if (command.equalsIgnoreCase("END")) {
                    break;
                }

                try {
                    QueryPlan plan = null;

                    plan = botaParser.parseQuery(command);

                    // execute the plan
                    Object result = executionEngine.execute(plan);

                    System.out.println(result.toString());

                    // print the result
                    printResult(result, plan, graphTraverser, graph);

                }
                catch (Exception e) {

                }
            }

        }
        catch (Exception ignored) {

        }
        finally {
            // run a checkpoint and save the snapshot
            checkpointManager.createCheckpoint(graph.getNodes(), graph.getEdges());
        }
    }

    private static String fmtNode(UUID id, Graph g) {
        Node node = g.getNodes().get(id).getNode();
        if (node == null) {
            return "[" + id + "]";
        }
        return "[" + node.getLabel() + ": " + id.toString().substring(0, 8) + "]";
    }

    private static String fmtEdge(UUID edgeId, Graph g) {
        Edge edge = g.getEdges()
                .get(edgeId)
                .getEdge();

        return "--" + edge.getType() + "-->";
    }

    private static void printPath(List<UUID> path, Graph g) {
        if (path == null || path.isEmpty()) {
            System.out.println("No path found");
            return;
        }

        for (int i = 0; i < path.size(); i++) {
            System.out.println(fmtNode(path.get(i), g));

            if (i <  path.size() - 1) {

                UUID from = path.get(i);
                UUID to = path.get(i + 1);

                for (UUID edge : g.getOutgoingAdjacencyList().getOrDefault(from, List.of())) {
                    Edge e = g.getEdges().get(edge).getEdge();

                    if (e.getTarget().equals(to)) {
                        System.out.println(" " + fmtEdge(edge, g) + " ");
                        break;
                    }
                }
            }
        }

        System.out.println();
    }

    private static void printNeighbors(TraversalResult result, Graph g) {
        List<TraversalNode> path = (List<TraversalNode>) result.getNodes();
        for (TraversalNode node : path) {
            System.out.println(fmtNode(node.nodeId, g));
        }
    }

    private static void printBfs(TraversalResult r, Graph g) {
        for (TraversalNode n : r.getNodes()) {
            System.out.println(fmtNode(n.nodeId, g));
        }
    }

    private static void printResult(Object result, QueryPlan plan, GraphTraverser traverser, Graph g) {
        if (result == null) {
            System.out.println("No Result");
            return;
        }

        switch(plan.operation) {
            case CREATE -> {
                switch (plan.targetType) {
                    case "NODE" -> {
                        System.out.println(result);
                    }
                }
            }
            case FIND -> {
                switch (plan.strategy) {
                    case BFS -> {
                        printBfs((TraversalResult) result, g);
                    }
                    case NEIGHBOURS -> {
                        printNeighbors((TraversalResult) result, g);
                    }
                    case SHORTEST_PATH -> {
                        TraversalResult tr = (TraversalResult) result;

                        List<UUID> path = traverser.reconstructPath(tr, plan.startId, plan.endId);

                        printPath(path, g);
                        break;
                    }
                }
            }
        }
    }
}