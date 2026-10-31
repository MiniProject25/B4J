package com.project.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GraphTest {

    private Graph graph;

    @BeforeEach
    public void setup() {
        graph = new Graph();
    }

    @Test
    public void testAddAndRemoveNode() {
        Node node1 = new Node("Person", Map.of("name", "Alice"));
        Node node2 = new Node("Person", Map.of("name", "Bob"));

        graph.addNode(node1);
        graph.addNode(node2);

        assertEquals(2, graph.getNodes().size());
        assertTrue(graph.getNodes().containsKey(node1.getId()));

        Edge edge = new Edge("KNOWS", node1.getId(), node2.getId());
        graph.addEdge(edge);

        assertEquals(1, graph.getEdges().size());
        assertTrue(graph.getOutgoingAdjacencyList().get(node1.getId()).contains(edge.getId()));
        assertTrue(graph.getIncomingAdjacencyList().get(node2.getId()).contains(edge.getId()));

        // Removing node1 should cascade remove the edge from node2's incoming list as well!
        graph.removeNode(node1.getId());

        assertEquals(1, graph.getNodes().size());
        assertEquals(0, graph.getEdges().size());
        assertFalse(graph.getIncomingAdjacencyList().get(node2.getId()).contains(edge.getId()));
    }

    @Test
    public void testRemoveEdge() {
        Node n1 = new Node("A");
        Node n2 = new Node("B");
        graph.addNode(n1);
        graph.addNode(n2);

        Edge e = new Edge("CONNECTS", n1.getId(), n2.getId());
        graph.addEdge(e);

        assertEquals(1, graph.getEdges().size());

        graph.removeEdge(e.getId());

        assertEquals(0, graph.getEdges().size());
        assertFalse(graph.getOutgoingAdjacencyList().get(n1.getId()).contains(e.getId()));
        assertFalse(graph.getIncomingAdjacencyList().get(n2.getId()).contains(e.getId()));
    }

    @Test
    public void testModifyProperties() {
        Node n = new Node("Item");
        graph.addNode(n);

        graph.modifyNodeProperty(n.getId(), "color", "red");
        assertEquals("red", graph.getNodes().get(n.getId()).getNode().getProperties().get("color"));

        graph.removeNodeProperty(n.getId(), "color");
        assertFalse(graph.getNodes().get(n.getId()).getNode().getProperties().containsKey("color"));
    }

    @Test
    public void testEdgeWithNonExistentNodeFails() {
        Node n1 = new Node("X");
        graph.addNode(n1);

        Edge e = new Edge("LINKS", n1.getId(), UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () -> graph.addEdge(e));
    }
}
