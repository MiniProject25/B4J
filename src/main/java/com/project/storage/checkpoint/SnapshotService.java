package com.project.storage.checkpoint;

import com.project.core.Edge;
import com.project.core.EdgeRecord;
import com.project.core.Node;
import com.project.core.NodeRecord;
import com.project.storage.io.EdgeRecordWriter;
import com.project.storage.io.NodeRecordWriter;
import com.project.storage.wal.Operations;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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

    public synchronized void writeSnapshot(Map<UUID, NodeRecord> nodes, Map<UUID, EdgeRecord> edges) throws IOException {
        File nodeTmp = new File(nodeFile + ".tmp");
        File edgeTmp = new File(edgeFile + ".tmp");

        File parent = nodeTmp.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (DataOutputStream nodeOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(nodeTmp)))) {
            for (Map.Entry<UUID, NodeRecord> entry : nodes.entrySet()) {
                Node node = entry.getValue().getNode();
                if (node == null) continue;
                nodeRecordWriter.serializeNode(nodeOut, Operations.ADD_NODE, node.getId(), node.getLabel(), node.getProperties());
            }
            nodeOut.flush();
        }

        try (DataOutputStream edgeOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(edgeTmp)))) {
            for (Map.Entry<UUID, EdgeRecord> entry : edges.entrySet()) {
                Edge edge = entry.getValue().getEdge();
                if (edge == null) continue;
                edgeRecordWriter.serializeEdge(edgeOut, Operations.ADD_EDGE, edge.getId(), edge.getType(), edge.getSource(), edge.getTarget(), edge.getProperties());
            }
            edgeOut.flush();
        }

        Files.move(nodeTmp.toPath(), Path.of(nodeFile), StandardCopyOption.REPLACE_EXISTING);
        Files.move(edgeTmp.toPath(), Path.of(edgeFile), StandardCopyOption.REPLACE_EXISTING);
    }
}
