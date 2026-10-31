package com.project.server;

import com.project.config.db.Config;
import com.project.core.Graph;
import com.project.storage.GraphStorageEngine;
import com.project.storage.loader.GraphLoader;

import java.io.File;
import java.io.IOException;

public class BootManager {

    private final Graph graph;
    private final GraphStorageEngine graphStorageEngine;
    private final GraphLoader graphLoader;
    private final Config config;

    public BootManager(GraphStorageEngine graphStorageEngine, GraphLoader graphLoader, Graph graph, Config config) {
        this.graphStorageEngine = graphStorageEngine;
        this.graphLoader = graphLoader;
        this.graph = graph;
        this.config = config;
    }

    public BootManager(GraphStorageEngine graphStorageEngine, GraphLoader graphLoader, Graph graph) {
        this(graphStorageEngine, graphLoader, graph, null);
    }

    public Graph boot(String dbName) throws IOException {
        String walPath = (config != null) ? config.walPath() : "b4j-data/" + dbName + "/wal.chk";
        String nodePath = (config != null) ? config.nodesPath() : "b4j-data/" + dbName + "/nodes.chk";
        String edgePath = (config != null) ? config.edgesPath() : "b4j-data/" + dbName + "/edges.chk";

        File wal = new File(walPath);
        boolean isWalAvail = wal.exists() && wal.length() > 0;

        // Perform recovery if WAL has unflushed records
        if (isWalAvail) {
            System.out.println("Detected unflushed WAL records. Initiating recovery...");
            graphStorageEngine.recoverFromWal(dbName);
            System.out.println("Recovery successful! Replayed WAL onto graph.");
            System.out.println("Boot completed.");
            return graph;
        }

        // Check if existing snapshot is available
        File node = new File(nodePath);
        File edge = new File(edgePath);
        boolean isSnapshotAvail = (node.exists() && node.length() > 0) || (edge.exists() && edge.length() > 0);

        if (isSnapshotAvail) {
            System.out.println("Loading graph snapshot into memory...");
            graphLoader.load(graph, dbName);
            System.out.println("Successfully loaded graph snapshot (" + graph.getNodes().size() + " nodes, " + graph.getEdges().size() + " edges).");
        } else {
            System.out.println("No existing snapshot found. Starting with empty graph.");
        }

        System.out.println("Boot completed.");
        return graph;
    }
}
