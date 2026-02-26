package com.project.traversal;

import com.project.core.Edge;
import com.project.core.Graph;

import java.util.*;

public class Bfs {

    public static List<UUID> findPath(UUID startId, UUID endId, Graph graph) {
        Queue<UUID> q = new LinkedList<>();
        Set<UUID> visited = new HashSet<>();
        Map<UUID, UUID> cameFrom = new HashMap<>();

        q.add(startId);
        visited.add(startId);

        while (!q.isEmpty()) {
            UUID u = q.poll();

            // check if it's target
            if (endId.equals(u)) {
                return reconstructPath(cameFrom, u);
            }

            // otherwise traverse the neighbors of u
            for (UUID edge : graph.getOutgoingAdjacencyList().get(u)) {

            }
        }

        return new ArrayList<>();
    }

    private static List<UUID> reconstructPath(Map<UUID, UUID> cameFrom, UUID u) {
        return new ArrayList<>();
    }
}
