package com.project.query;

/*
    FIND NODES FROM <UUID> DEPTH <n> --> BFS
    FIND NODES FROM <UUID> DEPTH <n> WHERE NODE_LABEL <label> AND EDGE_LABEL <label> DEPTH <n> --> DFS
    FIND PATH FROM <UUID> TO <UUID> -- BFS Shortest Path
    FIND PATH FROM <UUID> TO <UUID> WHERE EDGE_LABEL <label>
    FIND NEIGHBOURS OF <UUID>
    FIND NEIGHBOURS OF <UUID> WHERE EDGE_LABEL <label>

    CREATE NODE <LABEL> {key1: "value1", key2: 123}
    CREATE EDGE <LABEL> FROM <sourceUUID> TO <destUUID> {key1: "value1"}


 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.project.core.Edge;
import com.project.traversal.QueryPlan;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class BotaParser {

    public String[] smartTokenize(String query) {
        String[] naive = query.trim().split("\\s+");
//        System.out.println("Naive Split: " + Arrays.toString(naive));
        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < naive.length; i++) {
            String token = naive[i];

            // if token starts with { but doesn't end with }, it is a multi-token json block
            if (token.startsWith("{") && !token.endsWith("}")) {
                StringBuilder jsonBlock = new StringBuilder();

                // keep appending token until we find }
                jsonBlock.append(token);
                while(++i < naive.length && !naive[i].endsWith("}")) {
                    jsonBlock.append(" ").append(naive[i]);
                }

                if (i < naive.length) {
                    jsonBlock.append(" ").append(naive[i]); // adding the final }
                }
                tokens.add(jsonBlock.toString());
            }
            // handling quoted tokens getting split
            else if (token.startsWith("\"") && !token.endsWith("\"") && !token.endsWith("\"}")) {
                StringBuilder quoted = new StringBuilder();
                while(++i < naive.length) {
                    quoted.append(" ").append(naive[i]);
                    if (quoted.toString().endsWith("\"") || quoted.toString().endsWith("\"}")) {
                        break;
                    }
                }
                tokens.add(quoted.toString());
            }
            else {
                tokens.add(token);
            }
        }

        return tokens.toArray(new String[0]);
    }

    public QueryPlan parseQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        String[] tokens = smartTokenize(query);
//        System.out.println("Tokens: " + Arrays.toString(tokens));
        String command = tokens[0].toUpperCase();

        QueryPlan plan = new QueryPlan();

        switch (command) {
            case "FIND" -> {
                plan = parseFind(tokens, plan);
            }
            case "CREATE" -> {
                plan = parseCreate(tokens, plan);
            }
//            case "DELETE" -> {
//                plan = parseDelete(tokens, plan);
//            }
//            case "SHOW" -> {
//                plan = parseShow(tokens, plan);
//            }
            default -> {
                throw new IllegalArgumentException("Unknown command: " + command);
            }
        }

        return plan;
    }

    public QueryPlan parseCreate(String[] tokens, QueryPlan plan) {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("CREATE requires NODE OR EDGE");
        }

        plan.operation = QueryPlan.Operation.CREATE;

        String targetType = tokens[1].toUpperCase();
        plan.targetType = targetType;

        // CREATE NODE Person {"name" : "Sumith", "age" : "22"}
        // Tokens = CREATE, NODE, Person, {"name", :, "Sumith", , "age", :, "22"}

        if ("NODE".equalsIgnoreCase(targetType)) {
//            System.out.println("Entered NODE!");
            if (tokens.length < 3) {
                throw new IllegalArgumentException("NODE requires a LABEL (eg. Person, Company, etc.)");
            }

            plan.label = tokens[2]; // label

            String propsStr = String.join(" ", Arrays.copyOfRange(tokens, 3, tokens.length));
            // propsStr = {"name" : "Sumith", "age" : "22"}
//            System.out.println("Prop String: " + propsStr);
            plan.props = parseProperties(propsStr);
//            System.out.println("Plan.props: " + plan.props);
        }
        else if ("EDGE".equalsIgnoreCase(targetType)) {
            if (tokens.length < 7) {
                throw new IllegalArgumentException("EDGE requires LABEL, FROM, TO");
            }

            plan.label = tokens[2];
            if (!tokens[3].equalsIgnoreCase("FROM")) throw new IllegalArgumentException("Expected FROM");
            if (!tokens[4].equalsIgnoreCase("TO")) throw new IllegalArgumentException("Expected TO");

            if (tokens.length > 7) {
                String propStr = String.join(" ", Arrays.copyOfRange(tokens, 7, tokens.length));
                plan.props = parseProperties(propStr);
            }
        }
        else {
            throw new IllegalArgumentException("Unknown CREATE target: " + targetType);
        }

        return plan;
    }

    private Map<String, Object> parseProperties(String props) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> v = objectMapper.readValue(props, Map.class);

            // check if the values are of appropriate data type

            return v;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

//    public QueryPlan parseDelete(String[] tokens, QueryPlan plan) {
//
//    }
//
//    public QueryPlan parseShow(String[] tokens, QueryPlan plan) {
//
//    }

    public QueryPlan parseFind(String[] tokens, QueryPlan plan) {

        if (tokens[0].equalsIgnoreCase("FIND") &&
            tokens[1].equalsIgnoreCase("NODES")) {

            // BFS
            parseBfs(tokens, plan);
        }

        else if (tokens[0].equalsIgnoreCase("FIND") &&
            tokens[1].equalsIgnoreCase("PATH")) {

            // BFS - Shortest Path
            parseShortestPath(tokens, plan);
        }

        else if (tokens[0].equalsIgnoreCase("FIND") &&
            tokens[1].equalsIgnoreCase("NEIGHBOURS") || tokens[1].equalsIgnoreCase("NEIGHBORS")) {

            // Find neighbours of a node
            parseNeighbors(tokens, plan);
        }

        else {
            throw new IllegalArgumentException("Unknown Command " + tokens[1]);
        }

        return plan;
    }

    private void parseNeighbors(String[] tokens, QueryPlan traversalPlan) {
        traversalPlan.operation = QueryPlan.Operation.FIND;
        traversalPlan.strategy = QueryPlan.Strategy.NEIGHBOURS;

        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].toUpperCase()) {
                case "OF" -> {
                    traversalPlan.startId = UUID.fromString(tokens[++i]);
                }
                case "NODE_LABEL" -> {
                    traversalPlan.nodeFilter = tokens[++i];
                }
                case "EDGE_LABEL" -> {
                    traversalPlan.edgeFilter = tokens[++i];
                }
            }
        }
    }

    private void parseBfs(String[] tokens, QueryPlan traversalPlan) {
        traversalPlan.operation = QueryPlan.Operation.FIND;
        traversalPlan.strategy = QueryPlan.Strategy.BFS;

        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].toUpperCase()) {
                case "FROM" -> {
                    traversalPlan.startId = UUID.fromString(tokens[++i]);
                }
                case "DEPTH" -> {
                    traversalPlan.depth = Integer.parseInt(tokens[++i]);
                }
                case "NODE_LABEL" -> {
                    traversalPlan.nodeFilter = tokens[++i];
                }
                case "EDGE_LABEL" -> {
                    traversalPlan.edgeFilter = tokens[++i];
                }
            }
        }
    }

    private void parseShortestPath(String[] tokens, QueryPlan traversalPlan) {
        traversalPlan.operation = QueryPlan.Operation.FIND;
        traversalPlan.strategy = QueryPlan.Strategy.SHORTEST_PATH;

        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i].toUpperCase()) {
                case "FROM" -> {
                    traversalPlan.startId = UUID.fromString(tokens[++i]);
                }
                case "TO" -> {
                    traversalPlan.endId = UUID.fromString(tokens[++i]);
                }
                case "EDGE_LABEL" -> {
                    traversalPlan.edgeFilter = tokens[++i];
                }
            }
        }
    }
}
