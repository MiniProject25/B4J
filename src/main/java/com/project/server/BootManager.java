package com.project.server;

import com.project.core.Graph;
import com.project.storage.GraphStorageEngine;
import com.project.storage.loader.GraphLoader;

import java.io.File;
import java.io.IOException;

public class BootManager {

    private Graph graph;
    private final GraphStorageEngine graphStorageEngine;
    private final GraphLoader graphLoader;

    public BootManager(GraphStorageEngine graphStorageEngine, GraphLoader graphLoader, Graph graph) {
        this.graphStorageEngine = graphStorageEngine;
        this.graphLoader = graphLoader;
        this.graph = graph;
    }

    public Graph boot(String dbName) throws IOException {
        // 1. Check if there's any data in WAL. If yes, then append to .chk files
        File wal = new File("b4j-data/" + dbName+ "/wal.chk");
        boolean isWalAvail = wal.exists() && wal.length() > 0;

        // perform recovery
        if (isWalAvail) {
            System.out.println("Detected corruption...");
            System.out.println("Recovery Initiated.");
            graph = graphStorageEngine.recoverFromWal(dbName);
            System.out.println("Recovery Successful!");
            return graph;
        }

        // 2. Check if existing snapshot available. If yes, load this snapshot.
        File node = new File("b4j-data/" + dbName + "/nodes.chk");
        File edge = new File("b4j-data/" + dbName + "/edges.chk");

        boolean isSnapshotAvail = (node.exists() && node.length() > 0) || (edge.exists() && edge.length() > 0);

        if (isSnapshotAvail) {
            System.out.println("Detected snapshot...");
            System.out.println("Loading graph into memory.");
            graph = graphLoader.load(dbName);
            System.out.println("Successfully loaded graph!");
            System.out.println("Boot completed.");
            return graph;
        }
        else {
            System.out.println("Nothing to recover. Loading empty graph.");
        }

        System.out.println("Boot completed.");
        return graph;
    }
}
