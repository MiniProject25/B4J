package com.project.traversal;

import com.project.core.Edge;
import com.project.core.Graph;
import com.project.core.Node;
import com.project.traversal.filters.EdgeTypeFilter;
import com.project.traversal.filters.NodeTypeFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class GraphTraverserTest {

    private Graph graph;
    private GraphTraverser traverser;
    private Node a, b, c, d;
    private Edge ab, bc, cd;

    @BeforeEach
    public void setup() {
        graph = new Graph();
        traverser = new GraphTraverser(graph);

        // A -[KNOWS]-> B -[WORKS_AT]-> C -[KNOWS]-> D
        a = new Node("Person");
        b = new Node("Person");
        c = new Node("Company");
        d = new Node("Person");

        graph.addNode(a);
        graph.addNode(b);
        graph.addNode(c);
        graph.addNode(d);

        ab = new Edge("KNOWS", a.getId(), b.getId());
        bc = new Edge("WORKS_AT", b.getId(), c.getId());
        cd = new Edge("KNOWS", c.getId(), d.getId());

        graph.addEdge(ab);
        graph.addEdge(bc);
        graph.addEdge(cd);
    }

    @Test
    public void testNeighbors() {
        List<UUID> neighbors = traverser.neighbors(a.getId(), null);
        assertEquals(1, neighbors.size());
        assertEquals(b.getId(), neighbors.get(0));
    }

    @Test
    public void testBfsTraversal() {
        TraversalResult result = traverser.bfs(a.getId(), 2, null);
        assertNotNull(result);
        assertEquals(3, result.getNodes().size()); // A (depth 0), B (depth 1), C (depth 2)
    }

    @Test
    public void testShortestPath() {
        TraversalResult result = traverser.shortestPath(a.getId(), d.getId(), null);
        assertNotNull(result);

        List<UUID> path = traverser.reconstructPath(result, a.getId(), d.getId());
        assertEquals(List.of(a.getId(), b.getId(), c.getId(), d.getId()), path);
    }

    @Test
    public void testFilterEdgeType() {
        EdgeTypeFilter filter = new EdgeTypeFilter("KNOWS", graph);
        // From B, outgoing is WORKS_AT, so with KNOWS filter it should find no neighbors
        List<UUID> neighbors = traverser.neighbors(b.getId(), filter);
        assertTrue(neighbors.isEmpty());

        // From A, outgoing is KNOWS, so it finds B
        List<UUID> aNeighbors = traverser.neighbors(a.getId(), filter);
        assertEquals(1, aNeighbors.size());
    }

    @Test
    public void testFilterNodeType() {
        NodeTypeFilter filter = new NodeTypeFilter("Person", graph);
        // Traversing from A with maxDepth 3, but C is "Company", so it shouldn't proceed past B
        TraversalResult result = traverser.bfs(a.getId(), 3, filter);
        assertNotNull(result);
        assertTrue(result.getNodes().stream().allMatch(n -> n.nodeId.equals(a.getId()) || n.nodeId.equals(b.getId())));
    }
}
