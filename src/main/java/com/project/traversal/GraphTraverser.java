package com.project.traversal;

import com.project.core.Edge;
import com.project.core.EdgeRecord;
import com.project.core.Graph;
import com.project.traversal.filters.AllowAllFilter;
import com.project.traversal.filters.TraversalFilter;

import java.util.*;

public class GraphTraverser {
    private final Graph graph;

    public GraphTraverser(Graph graph) {
        this.graph = graph;
    }

    public List<UUID> neighbors(UUID nodeId, TraversalFilter filter) {
        if (filter == null) {
            filter = AllowAllFilter.INSTANCE;
        }

        List<UUID> n = new ArrayList<>();

        for (UUID e : graph.getOutgoingAdjacencyList().getOrDefault(nodeId, Collections.emptyList())) {
            if (!filter.allowEdge(e))
                continue;

            EdgeRecord edge = graph.getEdges().get(e);
            if (!filter.allowNode(edge.getEdge().getTarget()))
                continue;

            n.add(edge.getEdge().getTarget());
        }

        return n;
    }

    public TraversalResult bfs(UUID startId, int maxDepth, TraversalFilter filter) {
        Queue<TraversalNode> queue = new ArrayDeque<>();
        Map<UUID, TraversalNode> visited = new HashMap<>();

        TraversalNode root = new TraversalNode(startId, 0, null);

        queue.add(root);
        visited.put(startId, root);

        while (!queue.isEmpty()) {
            TraversalNode current = queue.poll();

            if (!filter.allowNode(current.nodeId)) {
                continue;
            }

            // check if depth is reached
            if (current.depth == maxDepth) continue;

            // go through the neighbors
            for (UUID id : graph.getOutgoingAdjacencyList().getOrDefault(current.nodeId, Collections.emptyList())) {
                Edge e = graph.getEdges().get(id).getEdge();

                if (!filter.allowEdge(e.getId())) continue;

                // check if the "to" part of the edge is already visited
                if (visited.containsKey(e.getTarget())) {
                    continue;
                }

                // if not, create a traversal node and add it to queue and visited
                TraversalNode nextNode = new TraversalNode(e.getTarget(), current.depth + 1, current.nodeId);

                queue.add(nextNode);
                visited.put(e.getTarget(), nextNode);
            }
        }

        return new TraversalResult(visited);
    }

    public TraversalResult shortestPath(UUID from, UUID to, TraversalFilter filter) {
        Queue<TraversalNode> queue = new ArrayDeque<>();
        Map<UUID, TraversalNode> visited = new HashMap<>();

        TraversalNode currNode = new TraversalNode(from, 0, null);

        queue.add(currNode);
        visited.put(from, currNode);

        while(!queue.isEmpty()) {
            TraversalNode current = queue.poll();

            if (!filter.allowNode(current.nodeId)) continue;

            if (current.nodeId.equals(to)) {
                return new TraversalResult(visited);
            }

            for (UUID id : graph.getOutgoingAdjacencyList().getOrDefault(current.nodeId, Collections.emptyList())) {
                Edge e = graph.getEdges().get(id).getEdge();

                if (!filter.allowEdge(e.getId())) continue;

                if (visited.containsKey(e.getTarget())) continue;

                TraversalNode node = new TraversalNode(e.getTarget(), current.depth + 1, current.nodeId);

                queue.add(node);
                visited.put(node.nodeId, node);
            }
        }

        return null;
    }

    public List<UUID> reconstructPath(TraversalResult tr, UUID start, UUID target) {
        // target not reached
        TraversalNode node = tr.get(target);
        if (node == null) return List.of();

        LinkedList<UUID> path = new LinkedList<>();

        UUID current = target;
        while(current != null) {
            path.addFirst(current);

            TraversalNode n = tr.get(current);
            current = (n != null) ? n.parentId : null;
        }

        if (!path.isEmpty() && !path.getFirst().equals(start)) {
            return List.of();
        }

        return path;
    }
}
