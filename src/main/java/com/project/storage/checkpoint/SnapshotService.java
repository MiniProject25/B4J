package com.project.storage.checkpoint;

import com.project.core.Edge;
import com.project.core.EdgeRecord;
import com.project.core.Node;
import com.project.core.NodeRecord;
import com.project.storage.io.EdgeRecordWriter;
import com.project.storage.io.NodeRecordWriter;
import com.project.storage.wal.Operations;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class SnapshotService {
    private final String nodeFile;
    private final String edgeFile;
    private final NodeRecordWriter nodeRecordWriter;
    private final EdgeRecordWriter edgeRecordWriter;

    public SnapshotService(String nodeFile, String edgeFile, NodeRecordWriter nodeRecordWriter, EdgeRecordWriter edgeRecordWriter) {
        this.nodeFile = nodeFile;
        this.edgeFile = edgeFile;
        this.nodeRecordWriter = nodeRecordWriter;
        this.edgeRecordWriter = edgeRecordWriter;
    }

    public void writeSnapshot(Map<UUID, NodeRecord> nodes, Map<UUID, EdgeRecord> edges) throws IOException {
        // truncate the files
        new FileOutputStream(nodeFile, false).close();
        new FileOutputStream(edgeFile, false).close();

        try {

            for (Map.Entry<UUID, NodeRecord> entry : nodes.entrySet()) {
                Operations op = entry.getValue().getOp();
                Node node = entry.getValue().getNode();

                switch (op) {
                    case ADD_NODE -> {
                        nodeRecordWriter.writeCreate(node.getId(), node.getLabel(), node.getProperties());
                    }
                    case UPDATE_NODE -> {
                        nodeRecordWriter.writeUpdate(node.getId(), node.getLabel(), node.getProperties());
                    }
                    case DELETE_NODE -> {
                        nodeRecordWriter.writeDelete(node.getId());
                    }
                }
            }

            for (Map.Entry<UUID, EdgeRecord> entry : edges.entrySet()) {
                Operations op = entry.getValue().getOp();
                Edge edge = entry.getValue().getEdge();

                switch (op) {
                    case ADD_EDGE -> {
                        edgeRecordWriter.writeCreate(edge.getId(), edge.getType(), edge.getSource(), edge.getTarget(), edge.getProperties());
                    }
                    case UPDATE_EDGE -> {
                        edgeRecordWriter.writeUpdate(edge.getId(), edge.getType(), edge.getSource(), edge.getTarget(), edge.getProperties());
                    }
                    case DELETE_EDGE -> {
                        edgeRecordWriter.writeDelete(edge.getId());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
