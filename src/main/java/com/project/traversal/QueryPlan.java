package com.project.traversal;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QueryPlan {

    public enum Operation {
        CREATE,
        FIND,
        SHOW,
        DELETE
    }

    public enum Strategy {
        BFS,
        SHORTEST_PATH,
        NEIGHBOURS,
    }

    public Operation operation;

    // ----------------- TRAVERSAL FIELDS ----------------- //
    public Strategy strategy;
    public UUID startId;
    public UUID endId;
    public int depth;
    public String nodeFilter;
    public String edgeFilter;
    // ----------------- TRAVERSAL FIELDS ----------------- //

    // ----------------- CREATE FIELDS ----------------- //
    public String targetType;
    public String label;
    public UUID fromID;
    public UUID toID;
    public Map<String, Object> props = new HashMap<>();
    // ----------------- CREATE FIELDS ----------------- //

    // ----------------- SHOW & DELETE FIELDS ----------------- //
    public String showTarget;
    public String deleteTarget;
    public String databaseName;
    public UUID targetId;
    // ----------------- SHOW & DELETE FIELDS ----------------- //
}
