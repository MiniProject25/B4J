package com.project.storage;

import com.project.core.*;
import com.project.storage.checkpoint.CheckpointManager;
import com.project.storage.loader.GraphLoader;
import com.project.storage.wal.Operations;
import com.project.storage.wal.WALWriter;
import com.project.storage.io.EdgeRecordWriter;
import com.project.storage.io.NodeRecordWriter;

import java.io.*;
import java.lang.reflect.InaccessibleObjectException;
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
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private volatile boolean isCheckpointing = false;
    boolean isRecovery;
    private final File walFile;

    private final long WAL_THRESHOLD = 1024 * 1024 * 100;

    public GraphStorageEngine(Graph graph, NodeRecordWriter nodeRecordWriter, EdgeRecordWriter edgeRecordWriter, WALWriter walWriter, GraphLoader graphLoader, CheckpointManager checkpointManager) {
        this.graph = graph;
        this.nodeRecordWriter = nodeRecordWriter;
        this.edgeRecordWriter = edgeRecordWriter;
        this.walWriter = walWriter;
        this.graphLoader = graphLoader;
        this.isRecovery = false;
        this.walFile = walWriter.getWalFile();
        this.checkpointManager = checkpointManager;
    }

    void setIsRecovery(boolean isRecovery) {
        this.isRecovery = isRecovery;
    }

    public void addNode(Node node) throws InaccessibleObjectException, IOException, InterruptedException {
        if (!isRecovery)
            walWriter.logNodeOperations(Operations.ADD_NODE, node.getId(), node.getLabel(), node.getProperties());
        graph.addNode(node);
        if (!isCheckpointing && walFile.length() >= WAL_THRESHOLD) {
            isCheckpointing = true;

            Map<UUID, NodeRecord> nodeRecords = new HashMap<>(graph.getNodes());
            Map<UUID, EdgeRecord> edgeRecords = new HashMap<>(graph.getEdges());

            executorService.submit(() -> {
                try {
                    checkpointManager.createCheckpoint(nodeRecords, edgeRecords);
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                finally {
                    isCheckpointing = false;
                }
            });
        }
//        nodeRecordWriter.writeCreate(node.getId(), node.getLabel(), node.getProperties());
    }

    public void addEdge(Edge edge) throws InaccessibleObjectException, IOException, InterruptedException {
        if (!isRecovery)
            walWriter.logEdgeOperations(Operations.ADD_EDGE, edge.getId(), edge.getSource(), edge.getTarget(), edge.getType(), edge.getProperties());
        graph.addEdge(edge);
        if (!isCheckpointing && walFile.length() >= WAL_THRESHOLD) {
            isCheckpointing = true;

            Map<UUID, NodeRecord> nodeRecords = new HashMap<>(graph.getNodes());
            Map<UUID, EdgeRecord> edgeRecords = new HashMap<>(graph.getEdges());

            executorService.submit(() -> {
                try {
                    checkpointManager.createCheckpoint(nodeRecords, edgeRecords);
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                finally {
                    isCheckpointing = false;
                }
            });
        }
//        edgeRecordWriter.writeCreate(edge.getId(), edge.getType(), edge.getSource(), edge.getTarget(), edge.getProperties());
    }

    public void modifyNode(UUID node, String key, Object value) throws InaccessibleObjectException, IOException, InterruptedException {
        Node updatedNode = graph.getNodes().get(node).getNode();
        if (!isRecovery)
            walWriter.logNodeOperations(Operations.UPDATE_NODE, updatedNode.getId(), updatedNode.getLabel(), updatedNode.getProperties());
        graph.modifyNodeProperty(node, key, value);
        if (!isCheckpointing && walFile.length() >= WAL_THRESHOLD) {
            isCheckpointing = true;

            Map<UUID, NodeRecord> nodeRecords = new HashMap<>(graph.getNodes());
            Map<UUID, EdgeRecord> edgeRecords = new HashMap<>(graph.getEdges());

            executorService.submit(() -> {
                try {
                    checkpointManager.createCheckpoint(nodeRecords, edgeRecords);
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                finally {
                    isCheckpointing = false;
                }
            });
        }
//        nodeRecordWriter.writeUpdate(updatedNode.getId(), updatedNode.getLabel(), updatedNode.getProperties());
    }

    public void modifyEdge(UUID edgeId, String key, Object value) throws InaccessibleObjectException, IOException, InterruptedException {
        Edge updatedEdge = graph.getEdges().get(edgeId).getEdge();
        if (!isRecovery)
            walWriter.logEdgeOperations(Operations.UPDATE_EDGE, edgeId, updatedEdge.getSource(), updatedEdge.getTarget(), updatedEdge.getType(), updatedEdge.getProperties());
        graph.modifyEdgeProperty(edgeId, key, value);
        if (!isCheckpointing && walFile.length() >= WAL_THRESHOLD) {
            isCheckpointing = true;

            Map<UUID, NodeRecord> nodeRecords = new HashMap<>(graph.getNodes());
            Map<UUID, EdgeRecord> edgeRecords = new HashMap<>(graph.getEdges());

            executorService.submit(() -> {
                try {
                    checkpointManager.createCheckpoint(nodeRecords, edgeRecords);
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                finally {
                    isCheckpointing = false;
                }
            });
        }
//        edgeRecordWriter.writeUpdate(updatedEdge.getId(), updatedEdge.getType(), updatedEdge.getSource(), updatedEdge.getTarget(), updatedEdge.getProperties());
    }

    public void deleteNode(UUID node) throws InaccessibleObjectException, IOException, InterruptedException {
        if (!isRecovery)
            walWriter.logNodeOperations(Operations.DELETE_NODE, node, null, null);
        graph.removeNode(node);
        if (!isCheckpointing && walFile.length() >= WAL_THRESHOLD) {
            isCheckpointing = true;

            Map<UUID, NodeRecord> nodeRecords = new HashMap<>(graph.getNodes());
            Map<UUID, EdgeRecord> edgeRecords = new HashMap<>(graph.getEdges());

            executorService.submit(() -> {
                try {
                    checkpointManager.createCheckpoint(nodeRecords, edgeRecords);
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                finally {
                    isCheckpointing = false;
                }
            });
        }
//        nodeRecordWriter.writeDelete(node);
    }

    public void deleteEdge(UUID edgeId) throws IOException, InterruptedException {
        if (!isRecovery)
            walWriter.logEdgeOperations(Operations.DELETE_EDGE, edgeId, null, null, null, null);
        graph.removeEdge(edgeId);
        if (!isCheckpointing && walFile.length() >= WAL_THRESHOLD) {
            isCheckpointing = true;

            Map<UUID, NodeRecord> nodeRecords = new HashMap<>(graph.getNodes());
            Map<UUID, EdgeRecord> edgeRecords = new HashMap<>(graph.getEdges());

            executorService.submit(() -> {
                try {
                    checkpointManager.createCheckpoint(nodeRecords, edgeRecords);
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }
                finally {
                    isCheckpointing = false;
                }
            });
        }
//        edgeRecordWriter.writeDelete(edgeId);
    }

    public Graph recoverFromWal(String dbName) throws IOException {
        setIsRecovery(true);
        String filename = "b4j-data/" + dbName + "/wal.chk";
        String nodeFile = "b4j-data/" + dbName + "/nodes.chk";
        String edgeFile = "b4j-data/" + dbName + "/edges.chk";

        Map<UUID, NodeRecord> nodes = new HashMap<>();
        Map<UUID, EdgeRecord> edges = new HashMap<>();

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)))) {
            if (Files.size(Path.of(filename)) <= 0) {
                System.out.println("Nothing to recover.");
                return null;
            }

            while(true) {
                byte opByte = in.readByte();
                Operations op = fromByte(opByte);

                switch(op) {
                    case ADD_NODE, UPDATE_NODE -> {
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

                        nodes.put(id, new NodeRecord(op, new Node(id, label, props)));
                    }
                    case DELETE_NODE -> {
                        UUID id = new UUID(in.readLong(), in.readLong());
                        nodes.remove(id);
                        //remove edges related to the id
                        edges.values().removeIf((edge -> edge.getEdge().getSource().equals(id) || edge.getEdge().getTarget().equals(id)));
                    }

                    case ADD_EDGE, UPDATE_EDGE -> {
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

                        edges.put(id, new EdgeRecord(op, new Edge(id, type, srcId, destId, props)));
                    }

                    case DELETE_EDGE -> {
                        UUID id = new UUID(in.readLong(), in.readLong());
                        edges.remove(id);
                    }

                }

            }

        }catch (EOFException ignored) {

        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        // rewriting .chk files (excluding wal.chk)
        // clear out the file
        new FileOutputStream(nodeFile, false).close();
        new FileOutputStream(edgeFile, false).close();

        // rewrite into the file
        try {
            for (NodeRecord nodeRecord : nodes.values()) {
                Operations op = nodeRecord.getOp();
                switch (op) {
                    case ADD_NODE -> {
                        nodeRecordWriter.writeCreate(nodeRecord.getNode().getId(), nodeRecord.getNode().getLabel(), nodeRecord.getNode().getProperties());
                    }
                    case UPDATE_NODE -> {
                        nodeRecordWriter.writeUpdate(nodeRecord.getNode().getId(), nodeRecord.getNode().getLabel(), nodeRecord.getNode().getProperties());
                    }
                    case DELETE_NODE -> {
                        nodeRecordWriter.writeDelete(nodeRecord.getNode().getId());
                    }
                }
            }

            for (EdgeRecord edgeRecord : edges.values()) {
                Operations op = edgeRecord.getOp();
                switch (op) {
                    case ADD_EDGE -> {
                        edgeRecordWriter.writeCreate(edgeRecord.getEdge().getId(), edgeRecord.getEdge().getType(), edgeRecord.getEdge().getSource(), edgeRecord.getEdge().getTarget(), edgeRecord.getEdge().getProperties());
                    }
                    case UPDATE_EDGE -> {
                        edgeRecordWriter.writeUpdate(edgeRecord.getEdge().getId(), edgeRecord.getEdge().getType(), edgeRecord.getEdge().getSource(), edgeRecord.getEdge().getTarget(), edgeRecord.getEdge().getProperties());
                    }
                    case DELETE_EDGE -> {
                        edgeRecordWriter.writeDelete(edgeRecord.getEdge().getId());
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            setIsRecovery(false);
        }

        return graphLoader.load(dbName);

    }

    public void shutdownExecutor() {
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            throw new RuntimeException(e);
        }
    }
}