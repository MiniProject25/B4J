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

    public Graph load() throws IOException {
        Graph graph = new Graph();

        loadNodes(graph);
        loadEdges(graph);

        return graph;
    }

    public void loadNodes(Graph graph) throws IOException {
        File file = new File("bota4j-data/nodes.chk");
        if (!file.exists()) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            while(true) {
                NodeRecord node = nodeRecordWriter.deserializeNode(in);

                switch(node.getOp()) {
                    case ADD_NODE:
                        graph.addNode(node.getNode());
                        break;

                    case UPDATE_NODE:
                        graph.setNodeProperty(node.getNode().getId(), node.getNode().getProperties());
                        break;

                    case DELETE_NODE:
                        graph.removeNode(node.getNode().getId());
                        break;
                }
            }
        } catch (EOFException ignored) {

        }
    }

    public void loadEdges(Graph graph) throws IOException {
        File file = new File("bota4j-data/edges.chk");
        if (!file.exists()) {
            return;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            while(true) {
                EdgeRecord edge = edgeRecordWriter.deserializeEdge(in);

                switch(edge.getOp()) {
                    case ADD_EDGE:
                        graph.addEdge(edge.getEdge());
                        break;

                    case UPDATE_EDGE:
                        graph.setEdgeProperty(edge.getEdge().getId(), edge.getEdge().getProperties());
                        break;

                    case DELETE_EDGE:
                        graph.removeEdge(edge.getEdge().getId());
                        break;
                }
            }
        } catch (EOFException ignored) {

        }
    }
}


