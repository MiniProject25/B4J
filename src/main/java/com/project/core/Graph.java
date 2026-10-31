package com.project.core;

import com.project.storage.wal.Operations;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@Data
public class Graph {
    private final Map<UUID, NodeRecord> nodes = new HashMap<>();
    private final Map<UUID, EdgeRecord> edges = new HashMap<>();
    private final Map<UUID, List<UUID>> outgoingAdjacencyList = new HashMap<>(); // node -> list of edges leaving the node
    private final Map<UUID, List<UUID>> incomingAdjacencyList = new HashMap<>(); // node -> list of edges entering the node

    // creating a node
    public void addNode(Node node) {
        if (node == null || node.getId() == null) {
            throw new IllegalArgumentException("Node and Node ID cannot be null");
        }
        nodes.put(node.getId(), new NodeRecord(Operations.ADD_NODE, node));
        outgoingAdjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
        incomingAdjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }

    // creating an edge
    public void addEdge(Edge edge) throws IllegalArgumentException {
        if (edge == null || edge.getId() == null) {
            throw new IllegalArgumentException("Edge and Edge ID cannot be null");
        }
        if (nodes.containsKey(edge.getSource()) && nodes.containsKey(edge.getTarget())) {
            edges.put(edge.getId(), new EdgeRecord(Operations.ADD_EDGE, edge));
            outgoingAdjacencyList.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getId());
            incomingAdjacencyList.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>()).add(edge.getId());
            return;
        }

        throw new IllegalArgumentException("Source or Destination node doesn't exist for edge: " + edge.getId());
    }

    // modify node property
    public void modifyNodeProperty(UUID node, String key, Object value) throws IllegalArgumentException {
        NodeRecord nr = nodes.get(node);
        if (nr == null || nr.getNode() == null) {
            throw new IllegalArgumentException("Node doesn't exist: " + node);
        }
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        Node nodeToModify = nr.getNode();
        if (nodeToModify.getProperties() == null) {
            nodeToModify.setProperties(new HashMap<>());
        }
        nodeToModify.getProperties().put(key, value);
    }

    public void setNodeProperty(UUID node, Map<String, Object> properties) throws IllegalArgumentException {
        NodeRecord nr = nodes.get(node);
        if (nr == null || nr.getNode() == null) {
            throw new IllegalArgumentException("Node doesn't exist: " + node);
        }
        nr.getNode().setProperties(properties != null ? new HashMap<>(properties) : new HashMap<>());
    }

    // modify edge property
    public void modifyEdgeProperty(UUID edge, String key, Object value) throws IllegalArgumentException {
        EdgeRecord er = edges.get(edge);
        if (er == null || er.getEdge() == null) {
            throw new IllegalArgumentException("Edge doesn't exist: " + edge);
        }
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        Edge edgeToModify = er.getEdge();
        if (edgeToModify.getProperties() == null) {
            edgeToModify.setProperties(new HashMap<>());
        }
        edgeToModify.getProperties().put(key, value);
    }

    public void setEdgeProperty(UUID edge, Map<String, Object> properties) throws IllegalArgumentException {
        EdgeRecord er = edges.get(edge);
        if (er == null || er.getEdge() == null) {
            throw new IllegalArgumentException("Edge doesn't exist: " + edge);
        }
        er.getEdge().setProperties(properties != null ? new HashMap<>(properties) : new HashMap<>());
    }

    // remove node property
    public void removeNodeProperty(UUID node, String key) throws IllegalArgumentException {
        NodeRecord nr = nodes.get(node);
        if (nr == null || nr.getNode() == null) {
            throw new IllegalArgumentException("Node doesn't exist: " + node);
        }
        Node nodeToRemove = nr.getNode();
        if (nodeToRemove.getProperties() != null) {
            nodeToRemove.getProperties().remove(key);
        }
    }

    // remove edge property
    public void removeEdgeProperty(UUID edge, String key) throws IllegalArgumentException {
        EdgeRecord er = edges.get(edge);
        if (er == null || er.getEdge() == null) {
            throw new IllegalArgumentException("Edge doesn't exist: " + edge);
        }
        Edge edgeToRemove = er.getEdge();
        if (edgeToRemove.getProperties() != null) {
            edgeToRemove.getProperties().remove(key);
        }
    }

    // remove node
    public void removeNode(UUID node) throws IllegalArgumentException {
        NodeRecord nr = nodes.get(node);
        if (nr == null || nr.getNode() == null) {
            throw new IllegalArgumentException("Node doesn't exist: " + node);
        }

        List<UUID> outgoing = outgoingAdjacencyList.get(node);
        if (outgoing != null) {
            for (UUID edgeId : new ArrayList<>(outgoing)) {
                removeEdge(edgeId);
            }
        }

        List<UUID> incoming = incomingAdjacencyList.get(node);
        if (incoming != null) {
            for (UUID edgeId : new ArrayList<>(incoming)) {
                removeEdge(edgeId);
            }
        }

        nodes.remove(node);
        incomingAdjacencyList.remove(node);
        outgoingAdjacencyList.remove(node);
    }

    // remove edge
    public void removeEdge(UUID edge) throws IllegalArgumentException {
        EdgeRecord er = edges.get(edge);
        if (er == null || er.getEdge() == null) {
            throw new IllegalArgumentException("Edge doesn't exist: " + edge);
        }
        Edge edgeToRemove = er.getEdge();
        List<UUID> outgoing = outgoingAdjacencyList.get(edgeToRemove.getSource());
        if (outgoing != null) {
            outgoing.remove(edge);
        }
        List<UUID> incoming = incomingAdjacencyList.get(edgeToRemove.getTarget());
        if (incoming != null) {
            incoming.remove(edge);
        }
        edges.remove(edge);
    }

    public void clear() {
        nodes.clear();
        edges.clear();
        outgoingAdjacencyList.clear();
        incomingAdjacencyList.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Graph Summary:\n");
        sb.append("Total Nodes: ").append(nodes.size()).append(", Total Edges: ").append(edges.size()).append("\n\n");

        sb.append("Nodes:\n");
        for (UUID nodeId : nodes.keySet()) {
            sb.append("  ").append(nodes.get(nodeId).getNode()).append("\n");
        }

        sb.append("Edges:\n");
        for (UUID edgeId : edges.keySet()) {
            sb.append("  ").append(edges.get(edgeId).getEdge()).append("\n");
        }

        sb.append("Connections:\n");
        for (UUID nodeId : nodes.keySet()) {
            sb.append("  Node ").append(nodeId).append("\n");
            sb.append("    Outgoing: ").append(outgoingAdjacencyList.getOrDefault(nodeId, Collections.emptyList())).append("\n");
            sb.append("    Incoming: ").append(incomingAdjacencyList.getOrDefault(nodeId, Collections.emptyList())).append("\n");
        }

        return sb.toString().trim();
    }
}
