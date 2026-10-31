package com.project.query;

import com.project.core.Graph;
import com.project.storage.GraphStorageEngine;
import com.project.storage.checkpoint.CheckpointManager;
import com.project.storage.checkpoint.SnapshotService;
import com.project.storage.io.EdgeRecordWriter;
import com.project.storage.io.NodeRecordWriter;
import com.project.storage.loader.GraphLoader;
import com.project.storage.wal.WALWriter;
import com.project.traversal.GraphTraverser;
import com.project.traversal.QueryPlan;
import com.project.traversal.TraversalResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutionEngineTest {

    private final String dbName = "test_engine_db";
    private File dbDir;
    private Graph graph;
    private GraphStorageEngine storageEngine;
    private ExecutionEngine engine;
    private BotaParser parser;
    private NodeRecordWriter nrw;
    private EdgeRecordWriter erw;
    private WALWriter wal;

    @BeforeEach
    public void setup() throws Exception {
        dbDir = new File("b4j-data/" + dbName);
        deleteRecursively(dbDir);
        dbDir.mkdirs();

        String nodesPath = "b4j-data/" + dbName + "/nodes.chk";
        String edgesPath = "b4j-data/" + dbName + "/edges.chk";
        String walPath = "b4j-data/" + dbName + "/wal.chk";

        graph = new Graph();
        nrw = new NodeRecordWriter(nodesPath);
        erw = new EdgeRecordWriter(edgesPath);
        wal = new WALWriter(walPath);

        GraphLoader loader = new GraphLoader(nrw, erw);
        SnapshotService ss = new SnapshotService(nodesPath, edgesPath, nrw, erw);
        CheckpointManager cm = new CheckpointManager(ss, wal);

        storageEngine = new GraphStorageEngine(graph, nrw, erw, wal, loader, cm, ss);
        GraphTraverser traverser = new GraphTraverser(graph);
        parser = new BotaParser();
        engine = new ExecutionEngine(graph, traverser, storageEngine);
    }

    @AfterEach
    public void cleanup() throws Exception {
        storageEngine.shutdownExecutor();
        wal.close();
        nrw.close();
        erw.close();
        deleteRecursively(dbDir);
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File c : file.listFiles()) {
                deleteRecursively(c);
            }
        }
        file.delete();
    }

    @Test
    public void testEndToEndWorkFlow() throws Exception {
        // 1. Create Node A
        QueryPlan planA = parser.parseQuery("CREATE NODE Person {\"name\": \"Alice\", \"age\": 30}");
        Object resultA = engine.execute(planA);
        assertTrue(resultA.toString().contains("Created Node [Person]"));
        assertEquals(1, graph.getNodes().size());
        UUID idA = graph.getNodes().keySet().iterator().next();

        // 2. Create Node B
        QueryPlan planB = parser.parseQuery("CREATE NODE Person {\"name\": \"Bob\", \"age\": 25}");
        Object resultB = engine.execute(planB);
        assertTrue(resultB.toString().contains("Created Node [Person]"));
        assertEquals(2, graph.getNodes().size());

        UUID idB = graph.getNodes().keySet().stream().filter(id -> !id.equals(idA)).findFirst().orElseThrow();

        // 3. Create Edge
        QueryPlan planEdge = parser.parseQuery("CREATE EDGE KNOWS FROM " + idA + " TO " + idB + " {\"since\": 2020}");
        Object resultEdge = engine.execute(planEdge);
        assertTrue(resultEdge.toString().contains("Created Edge --[KNOWS]-->"));
        assertEquals(1, graph.getEdges().size());

        // 4. Find Neighbours
        QueryPlan planNeighbours = parser.parseQuery("FIND NEIGHBOURS OF " + idA);
        @SuppressWarnings("unchecked")
        List<UUID> neighbours = (List<UUID>) engine.execute(planNeighbours);
        assertEquals(1, neighbours.size());
        assertEquals(idB, neighbours.get(0));

        // 5. Find Nodes BFS
        QueryPlan planBfs = parser.parseQuery("FIND NODES FROM " + idA + " DEPTH 1");
        TraversalResult tr = (TraversalResult) engine.execute(planBfs);
        assertEquals(2, tr.getNodes().size());

        // 6. Find Shortest Path
        QueryPlan planPath = parser.parseQuery("FIND PATH FROM " + idA + " TO " + idB);
        TraversalResult pathResult = (TraversalResult) engine.execute(planPath);
        assertNotNull(pathResult);

        // 7. Show commands
        assertTrue(engine.execute(parser.parseQuery("SHOW NODES")).toString().contains("Alice"));
        assertTrue(engine.execute(parser.parseQuery("SHOW EDGES")).toString().contains("KNOWS"));
        assertTrue(engine.execute(parser.parseQuery("SHOW GRAPH")).toString().contains("Total Nodes: 2"));

        // 8. Delete
        UUID edgeId = graph.getEdges().keySet().iterator().next();
        Object deleteEdgeRes = engine.execute(parser.parseQuery("DELETE EDGE " + edgeId));
        assertTrue(deleteEdgeRes.toString().contains("Deleted Edge"));
        assertEquals(0, graph.getEdges().size());

        Object deleteNodeRes = engine.execute(parser.parseQuery("DELETE NODE " + idA));
        assertTrue(deleteNodeRes.toString().contains("Deleted Node"));
        assertEquals(1, graph.getNodes().size());
    }
}
