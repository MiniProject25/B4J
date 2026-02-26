package com.project.core;

import com.project.storage.wal.Operations;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

@Data
@AllArgsConstructor
public class Graph {
    private final Map<UUID, NodeRecord> nodes = new HashMap<>();
    private final Map<UUID, EdgeRecord> edges = new HashMap<>();
    private final Map<UUID, List<UUID>> outgoingAdjacencyList = new HashMap<>(); // node -> list of edges leaving the node
    private final Map<UUID, List<UUID>> incomingAdjacencyList = new HashMap<>(); // node -> list of edges entering the node

    // creating a node
    public void addNode(Node node) {
        nodes.put(node.getId(), new NodeRecord(Operations.ADD_NODE, node));
        outgoingAdjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
        incomingAdjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
    }
    // creating an edge
    public void addEdge(Edge edge) throws IllegalArgumentException{

        if (nodes.containsKey(edge.getSource()) && nodes.containsKey(edge.getTarget())) {
            edges.put(edge.getId(), new EdgeRecord(Operations.ADD_EDGE, edge));
            outgoingAdjacencyList.get(edge.getSource()).add(edge.getId());
            incomingAdjacencyList.get(edge.getTarget()).add(edge.getId());
            return;
        }

        throw new IllegalArgumentException("Source or Destination doesn't exist.");
    }
    // modify node
    public void modifyNodeProperty(UUID node, String key, Object value) throws IllegalArgumentException{
        Node nodeToModify = nodes.get(node).getNode();
        if (nodeToModify == null) {
            throw new IllegalArgumentException("Node doesn't exist.");
        }
        if (key.isEmpty() || !nodeToModify.getProperties().containsKey(key)) {
           throw new IllegalArgumentException("Key doesn't exist.");
        }
        nodeToModify.getProperties().put(key, value);
    }
    public void setNodeProperty(UUID node, Map<String, Object> properties) throws IllegalArgumentException{
        Node nodeToModify = nodes.get(node).getNode();
        if (nodeToModify == null) {
            throw new IllegalArgumentException("Node doesn't exist.");
        }
        nodeToModify.setProperties(properties);
    }
    // modify edge
    public void modifyEdgeProperty(UUID edge, String key, Object value) throws IllegalArgumentException{
        Edge edgeToModify = edges.get(edge).getEdge();
        if (edgeToModify == null) {
            throw new IllegalArgumentException("Edge doesn't exist.");
        }
        if (key.isEmpty() || !edgeToModify.getProperties().containsKey(key)) {
            throw new IllegalArgumentException("Key doesn't exist.");
        }
        edgeToModify.getProperties().put(key, value);
        System.out.println("Updated property!");
    }
    public void setEdgeProperty(UUID edge, Map<String, Object> properties) throws IllegalArgumentException{
        Edge edgeToModify = edges.get(edge).getEdge();
        if (edgeToModify == null) {
            throw new IllegalArgumentException("Edge doesn't exist.");
        }
        edgeToModify.setProperties(properties);
    }
    // remove node property
    public void removeNodeProperty(UUID node, String key) throws IllegalArgumentException{
        Node nodeToRemove = nodes.get(node).getNode();
        if (nodeToRemove == null) {
            throw new IllegalArgumentException("Node doesn't exist.");
        }
        if (key.isEmpty() || !nodeToRemove.getProperties().containsKey(key)) {
            throw  new IllegalArgumentException("Key doesn't exist.");
        }
        nodeToRemove.getProperties().remove(key);
        System.out.println("Removed Node property!");
    }
    // remove edge property
    public void removeEdgeProperty(UUID edge, String key) throws IllegalArgumentException{
        Edge edgeToRemove = edges.get(edge).getEdge();
        if (edgeToRemove == null) {
            throw new IllegalArgumentException("Edge doesn't exist.");
        }
        if (key.isEmpty() || !edgeToRemove.getProperties().containsKey(key)) {
            throw new IllegalArgumentException("Key doesn't exist.");
        }
        edgeToRemove.getProperties().remove(key);
        System.out.println("Removed Edge property!");
    }
    // remove node
    public void removeNode(UUID node) throws IllegalArgumentException{
        Node nodeToRemove = nodes.get(node).getNode();
        if (nodeToRemove == null) {
           throw new IllegalArgumentException("Node doesn't exist.");
        }
        for (UUID edge : outgoingAdjacencyList.get(node)) {
            edges.remove(edge);
        }
        for (UUID edge : incomingAdjacencyList.get(node)) {
            edges.remove(edge);
        }
        nodes.remove(node);
        incomingAdjacencyList.remove(node);
        outgoingAdjacencyList.remove(node);
        System.out.println("Removed node!");
    }
    // remove edge
    public void removeEdge(UUID edge) throws IllegalArgumentException{
        Edge edgeToRemove = edges.get(edge).getEdge();
        if (edgeToRemove == null) {
            throw new IllegalArgumentException("Edge doesn't exist.");
        }
        incomingAdjacencyList.get(edgeToRemove.getSource()).remove(edge);
        outgoingAdjacencyList.get(edgeToRemove.getTarget()).remove(edge);
        edges.remove(edge);
    }
//    // view graph
//    public void viewGraph() {
//        for (Map.Entry<UUID, List<UUID>> entry : outgoingAdjacencyList.entrySet()) {
//            System.out.print(entry.getKey() + ": ");
//            for (UUID edge : entry.getValue()) {
//                System.out.print(edges.get(edge).getTarget() + ", ");
//            }
//            System.out.println();
//        }
//    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Graph:\n");

        sb.append("Nodes:\n");
        for (UUID nodeId : nodes.keySet()) {
            sb.append("  ").append(nodeId)
                    .append(" -> ").append(nodes.get(nodeId))
                    .append("\n");
        }

        sb.append("Edges:\n");
        for (UUID edgeId : edges.keySet()) {
            sb.append("  ").append(edgeId)
                    .append(" -> ").append(edges.get(edgeId))
                    .append("\n");
        }

        sb.append("Connections:\n");
        for (UUID nodeId : nodes.keySet()) {
            sb.append("  Node ").append(nodeId).append("\n");

            sb.append("    Outgoing: ");
            sb.append(outgoingAdjacencyList.getOrDefault(nodeId, Collections.emptyList()));
            sb.append("\n");

            sb.append("    Incoming: ");
            sb.append(incomingAdjacencyList.getOrDefault(nodeId, Collections.emptyList()));
            sb.append("\n");
        }

        return sb.toString();
    }
}
