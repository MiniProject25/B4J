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

        if (nodeId == null || !graph.getNodes().containsKey(nodeId)) {
            return Collections.emptyList();
        }

        List<UUID> n = new ArrayList<>();

        for (UUID e : graph.getOutgoingAdjacencyList().getOrDefault(nodeId, Collections.emptyList())) {
            EdgeRecord er = graph.getEdges().get(e);
            if (er == null || er.getEdge() == null) continue;

            if (!filter.allowEdge(e)) continue;

            UUID targetId = er.getEdge().getTarget();
            if (!filter.allowNode(targetId)) continue;

            n.add(targetId);
        }

        return n;
    }

    public TraversalResult bfs(UUID startId, int maxDepth, TraversalFilter filter) {
        if (filter == null) {
            filter = AllowAllFilter.INSTANCE;
        }

        if (startId == null || !graph.getNodes().containsKey(startId)) {
            return new TraversalResult(Collections.emptyMap());
        }

        Queue<TraversalNode> queue = new ArrayDeque<>();
        Map<UUID, TraversalNode> visited = new LinkedHashMap<>();

        TraversalNode root = new TraversalNode(startId, 0, null);
        queue.add(root);
        visited.put(startId, root);

        while (!queue.isEmpty()) {
            TraversalNode current = queue.poll();

            if (!filter.allowNode(current.nodeId)) {
                continue;
            }

            if (current.depth >= maxDepth) continue;

            for (UUID id : graph.getOutgoingAdjacencyList().getOrDefault(current.nodeId, Collections.emptyList())) {
                EdgeRecord er = graph.getEdges().get(id);
                if (er == null || er.getEdge() == null) continue;

                Edge e = er.getEdge();
                if (!filter.allowEdge(e.getId())) continue;

                UUID nextNodeId = e.getTarget();
                if (!filter.allowNode(nextNodeId)) continue;

                if (visited.containsKey(nextNodeId)) {
                    continue;
                }

                TraversalNode nextNode = new TraversalNode(nextNodeId, current.depth + 1, current.nodeId);
                queue.add(nextNode);
                visited.put(nextNodeId, nextNode);
            }
        }

        return new TraversalResult(visited);
    }

    public TraversalResult shortestPath(UUID from, UUID to, TraversalFilter filter) {
        if (filter == null) {
            filter = AllowAllFilter.INSTANCE;
        }

        if (from == null || to == null || !graph.getNodes().containsKey(from) || !graph.getNodes().containsKey(to)) {
            return null;
        }

        Queue<TraversalNode> queue = new ArrayDeque<>();
        Map<UUID, TraversalNode> visited = new HashMap<>();

        TraversalNode currNode = new TraversalNode(from, 0, null);
        queue.add(currNode);
        visited.put(from, currNode);

        if (from.equals(to)) {
            return new TraversalResult(visited);
        }

        while (!queue.isEmpty()) {
            TraversalNode current = queue.poll();

            if (!filter.allowNode(current.nodeId)) continue;

            for (UUID id : graph.getOutgoingAdjacencyList().getOrDefault(current.nodeId, Collections.emptyList())) {
                EdgeRecord er = graph.getEdges().get(id);
                if (er == null || er.getEdge() == null) continue;

                Edge e = er.getEdge();
                if (!filter.allowEdge(e.getId())) continue;

                UUID nextId = e.getTarget();
                if (!filter.allowNode(nextId)) continue;

                if (visited.containsKey(nextId)) continue;

                TraversalNode node = new TraversalNode(nextId, current.depth + 1, current.nodeId);
                queue.add(node);
                visited.put(nextId, node);

                if (nextId.equals(to)) {
                    return new TraversalResult(visited);
                }
            }
        }

        return null;
    }

    public List<UUID> reconstructPath(TraversalResult tr, UUID start, UUID target) {
        if (tr == null || start == null || target == null) return Collections.emptyList();

        TraversalNode node = tr.get(target);
        if (node == null) return Collections.emptyList();

        LinkedList<UUID> path = new LinkedList<>();
        UUID current = target;

        while (current != null) {
            path.addFirst(current);
            TraversalNode n = tr.get(current);
            current = (n != null) ? n.parentId : null;
        }

        if (!path.isEmpty() && !path.getFirst().equals(start)) {
            return Collections.emptyList();
        }

        return path;
    }
}
