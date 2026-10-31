package com.project.storage;

import com.project.core.*;
import com.project.storage.checkpoint.CheckpointManager;
import com.project.storage.checkpoint.SnapshotService;
import com.project.storage.loader.GraphLoader;
import com.project.storage.wal.Operations;
import com.project.storage.wal.WALWriter;
import com.project.storage.io.EdgeRecordWriter;
import com.project.storage.io.NodeRecordWriter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.project.storage.wal.Operations.fromByte;

public class GraphStorageEngine {
    private final Graph graph;
    private final NodeRecordWriter nodeRecordWriter;
    private final EdgeRecordWriter edgeRecordWriter;
    private final WALWriter walWriter;
    private final GraphLoader graphLoader;
    private final CheckpointManager checkpointManager;
    private final SnapshotService snapshotService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean isCheckpointing = false;
    private boolean isRecovery = false;
    private final File walFile;

    private final long WAL_THRESHOLD = 1024 * 1024 * 10; // 10MB threshold

    public GraphStorageEngine(Graph graph, NodeRecordWriter nodeRecordWriter, EdgeRecordWriter edgeRecordWriter,
                              WALWriter walWriter, GraphLoader graphLoader, CheckpointManager checkpointManager,
                              SnapshotService snapshotService) {
        this.graph = graph;
        this.nodeRecordWriter = nodeRecordWriter;
        this.edgeRecordWriter = edgeRecordWriter;
        this.walWriter = walWriter;
        this.graphLoader = graphLoader;
        this.checkpointManager = checkpointManager;
        this.snapshotService = snapshotService;
        this.isRecovery = false;
        this.walFile = walWriter.getWalFile();
    }

    public GraphStorageEngine(Graph graph, NodeRecordWriter nodeRecordWriter, EdgeRecordWriter edgeRecordWriter,
                              WALWriter walWriter, GraphLoader graphLoader, CheckpointManager checkpointManager) {
        this(graph, nodeRecordWriter, edgeRecordWriter, walWriter, graphLoader, checkpointManager, null);
    }

    public void setIsRecovery(boolean isRecovery) {
        this.isRecovery = isRecovery;
    }

    private void checkCheckpointThreshold() {
        if (!isCheckpointing && walFile != null && walFile.exists() && walFile.length() >= WAL_THRESHOLD) {
            isCheckpointing = true;
            Map<UUID, NodeRecord> nodeRecords = new HashMap<>(graph.getNodes());
            Map<UUID, EdgeRecord> edgeRecords = new HashMap<>(graph.getEdges());

            executorService.submit(() -> {
                try {
                    checkpointManager.createCheckpoint(nodeRecords, edgeRecords);
                } catch (Exception e) {
                    System.err.println("Background checkpoint error: " + e.getMessage());
                } finally {
                    isCheckpointing = false;
                }
            });
        }
    }

    public void addNode(Node node) throws IOException, InterruptedException {
        if (!isRecovery) {
            walWriter.logNodeOperations(Operations.ADD_NODE, node.getId(), node.getLabel(), node.getProperties());
        }
        graph.addNode(node);
        checkCheckpointThreshold();
    }

    public void addEdge(Edge edge) throws IOException, InterruptedException {
        if (!isRecovery) {
            walWriter.logEdgeOperations(Operations.ADD_EDGE, edge.getId(), edge.getSource(), edge.getTarget(), edge.getType(), edge.getProperties());
        }
        graph.addEdge(edge);
        checkCheckpointThreshold();
    }

    public void modifyNode(UUID node, String key, Object value) throws IOException, InterruptedException {
        graph.modifyNodeProperty(node, key, value);
        Node updatedNode = graph.getNodes().get(node).getNode();
        if (!isRecovery) {
            walWriter.logNodeOperations(Operations.UPDATE_NODE, updatedNode.getId(), updatedNode.getLabel(), updatedNode.getProperties());
        }
        checkCheckpointThreshold();
    }

    public void modifyEdge(UUID edgeId, String key, Object value) throws IOException, InterruptedException {
        graph.modifyEdgeProperty(edgeId, key, value);
        Edge updatedEdge = graph.getEdges().get(edgeId).getEdge();
        if (!isRecovery) {
            walWriter.logEdgeOperations(Operations.UPDATE_EDGE, edgeId, updatedEdge.getSource(), updatedEdge.getTarget(), updatedEdge.getType(), updatedEdge.getProperties());
        }
        checkCheckpointThreshold();
    }

    public void deleteNode(UUID node) throws IOException, InterruptedException {
        if (!isRecovery) {
            walWriter.logNodeOperations(Operations.DELETE_NODE, node, null, null);
        }
        graph.removeNode(node);
        checkCheckpointThreshold();
    }

    public void deleteEdge(UUID edgeId) throws IOException, InterruptedException {
        if (!isRecovery) {
            walWriter.logEdgeOperations(Operations.DELETE_EDGE, edgeId, null, null, null, null);
        }
        graph.removeEdge(edgeId);
        checkCheckpointThreshold();
    }

    public Graph recoverFromWal(String dbName) throws IOException {
        setIsRecovery(true);
        try {
            // 1. Load existing snapshot into memory first
            graphLoader.load(graph, dbName);

            String walFilename = "b4j-data/" + dbName + "/wal.chk";
            File walFile = new File(walFilename);

            if (!walFile.exists() || walFile.length() == 0) {
                return graph;
            }

            // 2. Replay all WAL operations sequentially
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(walFile)))) {
                while (true) {
                    byte opByte = in.readByte();
                    Operations op = fromByte(opByte);

                    switch (op) {
                        case ADD_NODE -> {
                            long msb = in.readLong();
                            long lsb = in.readLong();
                            UUID id = new UUID(msb, lsb);

                            String label = nodeRecordWriter.readString(in);
                            Map<String, Object> props = new HashMap<>();

                            int count = in.readInt();
                            for (int i = 0; i < count; i++) {
                                String key = nodeRecordWriter.readString(in);
                                String value = nodeRecordWriter.readString(in);
                                props.put(key, value);
                            }

                            graph.addNode(new Node(id, label, props));
                        }
                        case UPDATE_NODE -> {
                            long msb = in.readLong();
                            long lsb = in.readLong();
                            UUID id = new UUID(msb, lsb);

                            String label = nodeRecordWriter.readString(in);
                            Map<String, Object> props = new HashMap<>();

                            int count = in.readInt();
                            for (int i = 0; i < count; i++) {
                                String key = nodeRecordWriter.readString(in);
                                String value = nodeRecordWriter.readString(in);
                                props.put(key, value);
                            }

                            if (graph.getNodes().containsKey(id)) {
                                graph.setNodeProperty(id, props);
                            } else {
                                graph.addNode(new Node(id, label, props));
                            }
                        }
                        case DELETE_NODE -> {
                            UUID id = new UUID(in.readLong(), in.readLong());
                            if (graph.getNodes().containsKey(id)) {
                                graph.removeNode(id);
                            }
                        }

                        case ADD_EDGE -> {
                            UUID id = new UUID(in.readLong(), in.readLong());
                            UUID srcId = new UUID(in.readLong(), in.readLong());
                            UUID destId = new UUID(in.readLong(), in.readLong());
                            String type = edgeRecordWriter.readString(in);
                            Map<String, Object> props = new HashMap<>();
                            int count = in.readInt();
                            for (int i = 0; i < count; i++) {
                                String key = edgeRecordWriter.readString(in);
                                String value = edgeRecordWriter.readString(in);
                                props.put(key, value);
                            }

                            if (graph.getNodes().containsKey(srcId) && graph.getNodes().containsKey(destId)) {
                                graph.addEdge(new Edge(id, type, srcId, destId, props));
                            }
                        }
                        case UPDATE_EDGE -> {
                            UUID id = new UUID(in.readLong(), in.readLong());
                            UUID srcId = new UUID(in.readLong(), in.readLong());
                            UUID destId = new UUID(in.readLong(), in.readLong());
                            String type = edgeRecordWriter.readString(in);
                            Map<String, Object> props = new HashMap<>();
                            int count = in.readInt();
                            for (int i = 0; i < count; i++) {
                                String key = edgeRecordWriter.readString(in);
                                String value = edgeRecordWriter.readString(in);
                                props.put(key, value);
                            }

                            if (graph.getEdges().containsKey(id)) {
                                graph.setEdgeProperty(id, props);
                            } else if (graph.getNodes().containsKey(srcId) && graph.getNodes().containsKey(destId)) {
                                graph.addEdge(new Edge(id, type, srcId, destId, props));
                            }
                        }

                        case DELETE_EDGE -> {
                            UUID id = new UUID(in.readLong(), in.readLong());
                            if (graph.getEdges().containsKey(id)) {
                                graph.removeEdge(id);
                            }
                        }
                    }
                }
            } catch (EOFException ignored) {
                // Normal EOF reached for WAL
            }

            // 3. Save snapshot with recovered state
            if (snapshotService != null) {
                snapshotService.writeSnapshot(graph.getNodes(), graph.getEdges());
            }

            // 4. Truncate WAL file now that it has been recovered into snapshot
            walWriter.truncate();

            return graph;
        } finally {
            setIsRecovery(false);
        }
    }

    public void shutdownExecutor() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
