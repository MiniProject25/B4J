package com.project.storage;

import com.project.core.Edge;
import com.project.core.Graph;
import com.project.core.Node;
import com.project.server.BootManager;
import com.project.storage.checkpoint.CheckpointManager;
import com.project.storage.checkpoint.SnapshotService;
import com.project.storage.io.EdgeRecordWriter;
import com.project.storage.io.NodeRecordWriter;
import com.project.storage.loader.GraphLoader;
import com.project.storage.wal.WALWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StorageRecoveryTest {

    private final String dbName = "testdb";
    private File dbDir;

    @BeforeEach
    public void setup() {
        dbDir = new File("b4j-data/" + dbName);
        deleteRecursively(dbDir);
        dbDir.mkdirs();
    }

    @AfterEach
    public void cleanup() {
        deleteRecursively(dbDir);
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File c : file.listFiles()) {
                deleteRecursively(c);
            }
        }
        file.delete();
    }

    @Test
    public void testCheckpointAndReload() throws Exception {
        String nodesPath = "b4j-data/" + dbName + "/nodes.chk";
        String edgesPath = "b4j-data/" + dbName + "/edges.chk";
        String walPath = "b4j-data/" + dbName + "/wal.chk";

        Graph graph1 = new Graph();
        NodeRecordWriter nrw1 = new NodeRecordWriter(nodesPath);
        EdgeRecordWriter erw1 = new EdgeRecordWriter(edgesPath);
        WALWriter wal1 = new WALWriter(walPath);

        GraphLoader loader1 = new GraphLoader(nrw1, erw1);
        SnapshotService ss1 = new SnapshotService(nodesPath, edgesPath, nrw1, erw1);
        CheckpointManager cm1 = new CheckpointManager(ss1, wal1);

        GraphStorageEngine engine1 = new GraphStorageEngine(graph1, nrw1, erw1, wal1, loader1, cm1, ss1);

        Node n1 = new Node("User", Map.of("name", "Alice"));
        Node n2 = new Node("User", Map.of("name", "Bob"));
        engine1.addNode(n1);
        engine1.addNode(n2);
        Edge e1 = new Edge("FRIENDS", n1.getId(), n2.getId(), Map.of("weight", 5));
        engine1.addEdge(e1);

        // Perform checkpoint and close
        cm1.createCheckpoint(graph1.getNodes(), graph1.getEdges());
        engine1.shutdownExecutor();
        wal1.close();
        nrw1.close();
        erw1.close();

        // Reload in a fresh session
        Graph graph2 = new Graph();
        NodeRecordWriter nrw2 = new NodeRecordWriter(nodesPath);
        EdgeRecordWriter erw2 = new EdgeRecordWriter(edgesPath);
        WALWriter wal2 = new WALWriter(walPath);

        GraphLoader loader2 = new GraphLoader(nrw2, erw2);
        SnapshotService ss2 = new SnapshotService(nodesPath, edgesPath, nrw2, erw2);
        CheckpointManager cm2 = new CheckpointManager(ss2, wal2);
        GraphStorageEngine engine2 = new GraphStorageEngine(graph2, nrw2, erw2, wal2, loader2, cm2, ss2);
        BootManager bootManager2 = new BootManager(engine2, loader2, graph2);

        bootManager2.boot(dbName);

        assertEquals(2, graph2.getNodes().size());
        assertEquals(1, graph2.getEdges().size());
        assertTrue(graph2.getNodes().containsKey(n1.getId()));
        assertTrue(graph2.getNodes().containsKey(n2.getId()));
        assertTrue(graph2.getEdges().containsKey(e1.getId()));

        engine2.shutdownExecutor();
        wal2.close();
        nrw2.close();
        erw2.close();
    }

    @Test
    public void testWalCrashRecovery() throws Exception {
        String nodesPath = "b4j-data/" + dbName + "/nodes.chk";
        String edgesPath = "b4j-data/" + dbName + "/edges.chk";
        String walPath = "b4j-data/" + dbName + "/wal.chk";

        // Phase 1: Create initial snapshot with 1 node
        Graph graph1 = new Graph();
        NodeRecordWriter nrw1 = new NodeRecordWriter(nodesPath);
        EdgeRecordWriter erw1 = new EdgeRecordWriter(edgesPath);
        WALWriter wal1 = new WALWriter(walPath);
        GraphLoader loader1 = new GraphLoader(nrw1, erw1);
        SnapshotService ss1 = new SnapshotService(nodesPath, edgesPath, nrw1, erw1);
        CheckpointManager cm1 = new CheckpointManager(ss1, wal1);
        GraphStorageEngine engine1 = new GraphStorageEngine(graph1, nrw1, erw1, wal1, loader1, cm1, ss1);

        Node baseNode = new Node("Base", Map.of("key", "val0"));
        engine1.addNode(baseNode);
        cm1.createCheckpoint(graph1.getNodes(), graph1.getEdges());

        // Phase 2: Add more nodes into WAL without checkpointing (simulating crash)
        Node walNode = new Node("Recovered", Map.of("key", "val1"));
        engine1.addNode(walNode);

        // Abrupt shutdown without checkpoint (wal has data)
        engine1.shutdownExecutor();
        wal1.close();
        nrw1.close();
        erw1.close();

        // Verify wal.chk has content
        File walFile = new File(walPath);
        assertTrue(walFile.exists() && walFile.length() > 0);

        // Phase 3: Boot with recovery
        Graph graphRecovered = new Graph();
        NodeRecordWriter nrw2 = new NodeRecordWriter(nodesPath);
        EdgeRecordWriter erw2 = new EdgeRecordWriter(edgesPath);
        WALWriter wal2 = new WALWriter(walPath);
        GraphLoader loader2 = new GraphLoader(nrw2, erw2);
        SnapshotService ss2 = new SnapshotService(nodesPath, edgesPath, nrw2, erw2);
        CheckpointManager cm2 = new CheckpointManager(ss2, wal2);
        GraphStorageEngine engine2 = new GraphStorageEngine(graphRecovered, nrw2, erw2, wal2, loader2, cm2, ss2);
        BootManager bootManager = new BootManager(engine2, loader2, graphRecovered);

        bootManager.boot(dbName);

        // Both the base node from snapshot AND the wal node should be recovered!
        assertEquals(2, graphRecovered.getNodes().size());
        assertTrue(graphRecovered.getNodes().containsKey(baseNode.getId()));
        assertTrue(graphRecovered.getNodes().containsKey(walNode.getId()));

        engine2.shutdownExecutor();
        wal2.close();
        nrw2.close();
        erw2.close();
    }
}
