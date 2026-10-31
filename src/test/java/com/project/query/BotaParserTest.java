package com.project.query;

import com.project.traversal.QueryPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BotaParserTest {

    private BotaParser parser;

    @BeforeEach
    public void setup() {
        parser = new BotaParser();
    }

    @Test
    public void testTokenizeSimple() {
        String query = "FIND NODES FROM 123e4567-e89b-12d3-a456-426614174000 DEPTH 2";
        String[] tokens = parser.smartTokenize(query);
        assertArrayEquals(new String[]{"FIND", "NODES", "FROM", "123e4567-e89b-12d3-a456-426614174000", "DEPTH", "2"}, tokens);
    }

    @Test
    public void testTokenizeWithJsonProps() {
        String query = "CREATE NODE Person {\"name\": \"Alice\", \"age\": 30}";
        String[] tokens = parser.smartTokenize(query);
        assertEquals(4, tokens.length);
        assertEquals("CREATE", tokens[0]);
        assertEquals("NODE", tokens[1]);
        assertEquals("Person", tokens[2]);
        assertEquals("{\"name\": \"Alice\", \"age\": 30}", tokens[3]);
    }

    @Test
    public void testParseCreateNodeWithProperties() {
        QueryPlan plan = parser.parseQuery("CREATE NODE Person {\"name\": \"Bob\", \"age\": 25}");
        assertEquals(QueryPlan.Operation.CREATE, plan.operation);
        assertEquals("NODE", plan.targetType);
        assertEquals("Person", plan.label);
        assertEquals("Bob", plan.props.get("name"));
        assertEquals(25, plan.props.get("age"));
    }

    @Test
    public void testParseCreateNodeUnquotedJson() {
        QueryPlan plan = parser.parseQuery("CREATE NODE Person {name: 'Charlie', score: 100}");
        assertEquals("Person", plan.label);
        assertEquals("Charlie", plan.props.get("name"));
        assertEquals(100, plan.props.get("score"));
    }

    @Test
    public void testParseCreateNodeWithoutProperties() {
        QueryPlan plan = parser.parseQuery("CREATE NODE Location");
        assertEquals("NODE", plan.targetType);
        assertEquals("Location", plan.label);
        assertTrue(plan.props.isEmpty());
    }

    @Test
    public void testParseCreateEdge() {
        UUID src = UUID.randomUUID();
        UUID dst = UUID.randomUUID();
        String query = "CREATE EDGE KNOWS FROM " + src + " TO " + dst + " {\"since\": 2022}";
        QueryPlan plan = parser.parseQuery(query);

        assertEquals(QueryPlan.Operation.CREATE, plan.operation);
        assertEquals("EDGE", plan.targetType);
        assertEquals("KNOWS", plan.label);
        assertEquals(src, plan.fromID);
        assertEquals(dst, plan.toID);
        assertEquals(2022, plan.props.get("since"));
    }

    @Test
    public void testParseFindNodesBfs() {
        UUID src = UUID.randomUUID();
        String query = "FIND NODES FROM " + src + " DEPTH 3 WHERE NODE_LABEL Person AND EDGE_LABEL KNOWS";
        QueryPlan plan = parser.parseQuery(query);

        assertEquals(QueryPlan.Operation.FIND, plan.operation);
        assertEquals(QueryPlan.Strategy.BFS, plan.strategy);
        assertEquals(src, plan.startId);
        assertEquals(3, plan.depth);
        assertEquals("Person", plan.nodeFilter);
        assertEquals("KNOWS", plan.edgeFilter);
    }

    @Test
    public void testParseFindNeighbors() {
        UUID src = UUID.randomUUID();
        QueryPlan plan = parser.parseQuery("FIND NEIGHBOURS OF " + src + " WHERE EDGE_LABEL WORKS_AT");

        assertEquals(QueryPlan.Operation.FIND, plan.operation);
        assertEquals(QueryPlan.Strategy.NEIGHBOURS, plan.strategy);
        assertEquals(src, plan.startId);
        assertEquals("WORKS_AT", plan.edgeFilter);
    }

    @Test
    public void testParseFindPath() {
        UUID src = UUID.randomUUID();
        UUID dst = UUID.randomUUID();
        QueryPlan plan = parser.parseQuery("FIND PATH FROM " + src + " TO " + dst + " WHERE EDGE_LABEL CONNECTS");

        assertEquals(QueryPlan.Operation.FIND, plan.operation);
        assertEquals(QueryPlan.Strategy.SHORTEST_PATH, plan.strategy);
        assertEquals(src, plan.startId);
        assertEquals(dst, plan.endId);
        assertEquals("CONNECTS", plan.edgeFilter);
    }

    @Test
    public void testParseShowCommands() {
        assertEquals("GRAPH", parser.parseQuery("SHOW GRAPH").showTarget);
        assertEquals("NODES", parser.parseQuery("SHOW NODES").showTarget);
        assertEquals("EDGES", parser.parseQuery("SHOW EDGES").showTarget);
    }

    @Test
    public void testParseDelete() {
        UUID id = UUID.randomUUID();
        QueryPlan planNode = parser.parseQuery("DELETE NODE " + id);
        assertEquals(QueryPlan.Operation.DELETE, planNode.operation);
        assertEquals("NODE", planNode.deleteTarget);
        assertEquals(id, planNode.targetId);

        QueryPlan planEdge = parser.parseQuery("DELETE EDGE " + id);
        assertEquals(QueryPlan.Operation.DELETE, planEdge.operation);
        assertEquals("EDGE", planEdge.deleteTarget);
        assertEquals(id, planEdge.targetId);
    }

    @Test
    public void testParseHelp() {
        assertEquals(QueryPlan.Operation.HELP, parser.parseQuery("HELP").operation);
        assertEquals(QueryPlan.Operation.HELP, parser.parseQuery("?").operation);
    }

    @Test
    public void testInvalidQueries() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseQuery(""));
        assertThrows(IllegalArgumentException.class, () -> parser.parseQuery("INVALID_CMD"));
        assertThrows(IllegalArgumentException.class, () -> parser.parseQuery("CREATE"));
        assertThrows(IllegalArgumentException.class, () -> parser.parseQuery("CREATE EDGE foo"));
    }
}
