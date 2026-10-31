package com.project.storage.loader;

import com.project.core.EdgeRecord;
import com.project.core.Graph;
import com.project.core.NodeRecord;
import com.project.storage.io.EdgeRecordWriter;
import com.project.storage.io.NodeRecordWriter;

import java.io.*;

public class GraphLoader {

    private final NodeRecordWriter nodeRecordWriter;
    private final EdgeRecordWriter edgeRecordWriter;

    public GraphLoader(NodeRecordWriter nodeRecordWriter, EdgeRecordWriter edgeRecordWriter) {
        this.nodeRecordWriter = nodeRecordWriter;
        this.edgeRecordWriter = edgeRecordWriter;
    }

    public Graph load(String dbName) throws IOException {
        Graph graph = new Graph();
        load(graph, dbName);
        return graph;
    }

    public void load(Graph graph, String dbName) throws IOException {
        loadNodes(graph, dbName);
        loadEdges(graph, dbName);
    }

    public void loadNodes(Graph graph, String dbName) throws IOException {
        File file = new File("b4j-data/" + dbName + "/nodes.chk");
        if (!file.exists() || file.length() == 0) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            while (true) {
                NodeRecord node = nodeRecordWriter.deserializeNode(in);
                if (node == null || node.getNode() == null) break;

                switch (node.getOp()) {
                    case ADD_NODE:
                        graph.addNode(node.getNode());
                        break;
                    case UPDATE_NODE:
                        if (graph.getNodes().containsKey(node.getNode().getId())) {
                            graph.setNodeProperty(node.getNode().getId(), node.getNode().getProperties());
                        } else {
                            graph.addNode(node.getNode());
                        }
                        break;
                    case DELETE_NODE:
                        if (graph.getNodes().containsKey(node.getNode().getId())) {
                            graph.removeNode(node.getNode().getId());
                        }
                        break;
                }
            }
        } catch (EOFException ignored) {
            // Reached end of file
        }
    }

    public void loadEdges(Graph graph, String dbName) throws IOException {
        File file = new File("b4j-data/" + dbName + "/edges.chk");
        if (!file.exists() || file.length() == 0) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            while (true) {
                EdgeRecord edge = edgeRecordWriter.deserializeEdge(in);
                if (edge == null || edge.getEdge() == null) break;

                switch (edge.getOp()) {
                    case ADD_EDGE:
                        if (graph.getNodes().containsKey(edge.getEdge().getSource()) &&
                            graph.getNodes().containsKey(edge.getEdge().getTarget())) {
                            graph.addEdge(edge.getEdge());
                        }
                        break;
                    case UPDATE_EDGE:
                        if (graph.getEdges().containsKey(edge.getEdge().getId())) {
                            graph.setEdgeProperty(edge.getEdge().getId(), edge.getEdge().getProperties());
                        }
                        break;
                    case DELETE_EDGE:
                        if (graph.getEdges().containsKey(edge.getEdge().getId())) {
                            graph.removeEdge(edge.getEdge().getId());
                        }
                        break;
                }
            }
        } catch (EOFException ignored) {
            // Reached end of file
        }
    }
}
