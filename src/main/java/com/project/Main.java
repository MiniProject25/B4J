package com.project;

import com.project.core.Edge;
import com.project.core.Graph;
import com.project.core.Node;
import com.project.storage.*;
import com.project.storage.checkpoint.CheckpointManager;
import com.project.storage.checkpoint.SnapshotService;
import com.project.storage.loader.GraphLoader;
import com.project.storage.wal.WALWriter;
import com.project.storage.io.EdgeRecordWriter;
import com.project.storage.io.NodeRecordWriter;

import java.io.FileNotFoundException;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {

        try {
            Graph graph = new Graph();
            NodeRecordWriter nodeRecordWriter = new NodeRecordWriter("nodes.chk");
            EdgeRecordWriter edgeRecordWriter = new EdgeRecordWriter("edges.chk");
            WALWriter walWriter = new WALWriter("wal.chk");
            GraphLoader loader = new GraphLoader(nodeRecordWriter, edgeRecordWriter);
            SnapshotService snapshotService = new SnapshotService("bota4j-data/nodes.chk", "bota4j-data/edges.chk", nodeRecordWriter, edgeRecordWriter);
            CheckpointManager checkpointManager = new CheckpointManager(snapshotService, walWriter);
            GraphStorageEngine storageEngine = new GraphStorageEngine(graph, nodeRecordWriter, edgeRecordWriter, walWriter, loader, checkpointManager);

            for (int i = 0; i < 100; i++) {
                Node Sumith = new Node("Person", Map.of("name", "Sumith"));
//                Node Blud = new Node("Person", Map.of("name", "Blud"));

//                Edge edge1 = new Edge("Enemies", Sumith.getId(), Blud.getId());

                storageEngine.addNode(Sumith);
//                storageEngine.addNode(Blud);

//                storageEngine.addEdge(edge1);
                System.out.println("Added node " + i);
            }

            System.out.println(graph);
            checkpointManager.createCheckpoint(graph.getNodes(), graph.getEdges());


        }
        catch (Exception e) {
            System.out.println("Message: " + e.getMessage());
        }
    }
}