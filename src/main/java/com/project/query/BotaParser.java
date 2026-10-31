package com.project.query;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.traversal.QueryPlan;

import java.util.*;

public class BotaParser {

    private final ObjectMapper objectMapper;

    public BotaParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        this.objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    public String[] smartTokenize(String query) {
        if (query == null) return new String[0];
        List<String> tokens = new ArrayList<>();
        int i = 0;
        int n = query.length();

        while (i < n) {
            // Skip leading whitespace
            while (i < n && Character.isWhitespace(query.charAt(i))) {
                i++;
            }
            if (i >= n) break;

            char c = query.charAt(i);
            if (c == '{') {
                // Parse full JSON block including nested braces and strings
                int start = i;
                int braceCount = 0;
                boolean inQuote = false;
                char quoteChar = 0;

                while (i < n) {
                    char ch = query.charAt(i);
                    if (inQuote) {
                        if (ch == '\\' && i + 1 < n) {
                            i += 2;
                            continue;
                        }
                        if (ch == quoteChar) {
                            inQuote = false;
                        }
                    } else {
                        if (ch == '"' || ch == '\'') {
                            inQuote = true;
                            quoteChar = ch;
                        } else if (ch == '{') {
                            braceCount++;
                        } else if (ch == '}') {
                            braceCount--;
                            if (braceCount == 0) {
                                i++; // consume closing brace
                                break;
                            }
                        }
                    }
                    i++;
                }
                tokens.add(query.substring(start, i));
            } else if (c == '"' || c == '\'') {
                // Quoted string
                int start = i;
                char quoteChar = c;
                i++; // skip opening quote
                while (i < n) {
                    char ch = query.charAt(i);
                    if (ch == '\\' && i + 1 < n) {
                        i += 2;
                        continue;
                    }
                    if (ch == quoteChar) {
                        i++; // consume closing quote
                        break;
                    }
                    i++;
                }
                tokens.add(query.substring(start, i));
            } else {
                // Normal token delimited by whitespace or start of JSON
                int start = i;
                while (i < n && !Character.isWhitespace(query.charAt(i)) && query.charAt(i) != '{') {
                    i++;
                }
                tokens.add(query.substring(start, i));
            }
        }

        return tokens.toArray(new String[0]);
    }

    public QueryPlan parseQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        String[] tokens = smartTokenize(query);
        if (tokens.length == 0) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        String command = tokens[0].toUpperCase();
        QueryPlan plan = new QueryPlan();

        switch (command) {
            case "FIND" -> plan = parseFind(tokens, plan);
            case "CREATE" -> plan = parseCreate(tokens, plan);
            case "DELETE" -> plan = parseDelete(tokens, plan);
            case "SHOW" -> plan = parseShow(tokens, plan);
            case "HELP", "?" -> {
                plan.operation = QueryPlan.Operation.HELP;
            }
            default -> throw new IllegalArgumentException("Unknown command: '" + command + "'. Type HELP for syntax.");
        }

        return plan;
    }

    public QueryPlan parseCreate(String[] tokens, QueryPlan plan) {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("CREATE requires NODE or EDGE");
        }

        plan.operation = QueryPlan.Operation.CREATE;
        String targetType = tokens[1].toUpperCase();
        plan.targetType = targetType;

        if ("NODE".equals(targetType)) {
            // Expected: CREATE NODE <Label> [props]
            if (tokens.length < 3) {
                throw new IllegalArgumentException("CREATE NODE requires a LABEL (e.g. CREATE NODE Person {\"name\": \"Alice\"})");
            }

            plan.label = tokens[2];

            if (tokens.length > 3) {
                String propsStr = String.join(" ", Arrays.copyOfRange(tokens, 3, tokens.length));
                plan.props = parseProperties(propsStr);
            }
        } else if ("EDGE".equals(targetType)) {
            // Expected: CREATE EDGE <LABEL> FROM <sourceUUID> TO <destUUID> [props]
            if (tokens.length < 7) {
                throw new IllegalArgumentException("CREATE EDGE requires: CREATE EDGE <LABEL> FROM <sourceUUID> TO <destUUID> [properties]");
            }

            plan.label = tokens[2];

            if (!tokens[3].equalsIgnoreCase("FROM")) {
                throw new IllegalArgumentException("Expected 'FROM' after edge label, got: " + tokens[3]);
            }

            try {
                plan.fromID = UUID.fromString(tokens[4]);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid source UUID: '" + tokens[4] + "'");
            }

            if (!tokens[5].equalsIgnoreCase("TO")) {
                throw new IllegalArgumentException("Expected 'TO' after source UUID, got: " + tokens[5]);
            }

            try {
                plan.toID = UUID.fromString(tokens[6]);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid destination UUID: '" + tokens[6] + "'");
            }

            if (tokens.length > 7) {
                String propStr = String.join(" ", Arrays.copyOfRange(tokens, 7, tokens.length));
                plan.props = parseProperties(propStr);
            }
        } else {
            throw new IllegalArgumentException("Unknown CREATE target: '" + targetType + "'. Expected NODE or EDGE.");
        }

        return plan;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseProperties(String props) {
        if (props == null || props.trim().isEmpty()) {
            return new HashMap<>();
        }
        String trimmed = props.trim();
        if (trimmed.equals("{}")) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(trimmed, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON properties: " + e.getMessage(), e);
        }
    }

    public QueryPlan parseDelete(String[] tokens, QueryPlan plan) {
        if (tokens.length < 3) {
            throw new IllegalArgumentException("DELETE requires: DELETE NODE <UUID> or DELETE EDGE <UUID>");
        }
        plan.operation = QueryPlan.Operation.DELETE;
        plan.deleteTarget = tokens[1].toUpperCase();

        try {
            plan.targetId = UUID.fromString(tokens[2]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID: '" + tokens[2] + "'");
        }

        return plan;
    }

    public QueryPlan parseShow(String[] tokens, QueryPlan plan) {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("SHOW requires: SHOW GRAPH, SHOW NODES, or SHOW EDGES");
        }
        plan.operation = QueryPlan.Operation.SHOW;
        plan.showTarget = tokens[1].toUpperCase();
        return plan;
    }

    public QueryPlan parseFind(String[] tokens, QueryPlan plan) {
        if (tokens.length < 2) {
            throw new IllegalArgumentException("FIND requires NODES, PATH, or NEIGHBOURS");
        }

        String target = tokens[1].toUpperCase();
        if ("NODES".equals(target)) {
            parseBfs(tokens, plan);
        } else if ("PATH".equals(target)) {
            parseShortestPath(tokens, plan);
        } else if ("NEIGHBOURS".equals(target) || "NEIGHBORS".equals(target)) {
            parseNeighbors(tokens, plan);
        } else {
            throw new IllegalArgumentException("Unknown FIND command: '" + tokens[1] + "'. Expected NODES, PATH, or NEIGHBOURS.");
        }

        return plan;
    }

    private void parseNeighbors(String[] tokens, QueryPlan traversalPlan) {
        traversalPlan.operation = QueryPlan.Operation.FIND;
        traversalPlan.strategy = QueryPlan.Strategy.NEIGHBOURS;

        for (int i = 2; i < tokens.length; i++) {
            switch (tokens[i].toUpperCase()) {
                case "OF", "FROM" -> {
                    if (i + 1 < tokens.length) {
                        try {
                            traversalPlan.startId = UUID.fromString(tokens[++i]);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Invalid UUID in FIND NEIGHBOURS: '" + tokens[i] + "'");
                        }
                    }
                }
                case "NODE_LABEL" -> {
                    if (i + 1 < tokens.length) {
                        traversalPlan.nodeFilter = tokens[++i];
                    }
                }
                case "EDGE_LABEL" -> {
                    if (i + 1 < tokens.length) {
                        traversalPlan.edgeFilter = tokens[++i];
                    }
                }
            }
        }

        if (traversalPlan.startId == null) {
            throw new IllegalArgumentException("FIND NEIGHBOURS requires 'OF <UUID>' (e.g. FIND NEIGHBOURS OF <UUID>)");
        }
    }

    private void parseBfs(String[] tokens, QueryPlan traversalPlan) {
        traversalPlan.operation = QueryPlan.Operation.FIND;
        traversalPlan.strategy = QueryPlan.Strategy.BFS;
        traversalPlan.depth = 1; // Default depth 1

        for (int i = 2; i < tokens.length; i++) {
            switch (tokens[i].toUpperCase()) {
                case "FROM" -> {
                    if (i + 1 < tokens.length) {
                        try {
                            traversalPlan.startId = UUID.fromString(tokens[++i]);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Invalid UUID in FIND NODES: '" + tokens[i] + "'");
                        }
                    }
                }
                case "DEPTH" -> {
                    if (i + 1 < tokens.length) {
                        try {
                            traversalPlan.depth = Integer.parseInt(tokens[++i]);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Invalid DEPTH number: '" + tokens[i] + "'");
                        }
                    }
                }
                case "NODE_LABEL" -> {
                    if (i + 1 < tokens.length) {
                        traversalPlan.nodeFilter = tokens[++i];
                    }
                }
                case "EDGE_LABEL" -> {
                    if (i + 1 < tokens.length) {
                        traversalPlan.edgeFilter = tokens[++i];
                    }
                }
            }
        }

        if (traversalPlan.startId == null) {
            throw new IllegalArgumentException("FIND NODES requires 'FROM <UUID>' (e.g. FIND NODES FROM <UUID> DEPTH 2)");
        }
    }

    private void parseShortestPath(String[] tokens, QueryPlan traversalPlan) {
        traversalPlan.operation = QueryPlan.Operation.FIND;
        traversalPlan.strategy = QueryPlan.Strategy.SHORTEST_PATH;

        for (int i = 2; i < tokens.length; i++) {
            switch (tokens[i].toUpperCase()) {
                case "FROM" -> {
                    if (i + 1 < tokens.length) {
                        try {
                            traversalPlan.startId = UUID.fromString(tokens[++i]);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Invalid start UUID in FIND PATH: '" + tokens[i] + "'");
                        }
                    }
                }
                case "TO" -> {
                    if (i + 1 < tokens.length) {
                        try {
                            traversalPlan.endId = UUID.fromString(tokens[++i]);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Invalid destination UUID in FIND PATH: '" + tokens[i] + "'");
                        }
                    }
                }
                case "EDGE_LABEL" -> {
                    if (i + 1 < tokens.length) {
                        traversalPlan.edgeFilter = tokens[++i];
                    }
                }
                case "NODE_LABEL" -> {
                    if (i + 1 < tokens.length) {
                        traversalPlan.nodeFilter = tokens[++i];
                    }
                }
            }
        }

        if (traversalPlan.startId == null || traversalPlan.endId == null) {
            throw new IllegalArgumentException("FIND PATH requires 'FROM <sourceUUID> TO <targetUUID>'");
        }
    }
}
