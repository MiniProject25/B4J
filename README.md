# (B4J)

A lightweight embedded graph database in Java featuring an in-memory graph engine, Write-Ahead Logging (WAL), snapshot checkpointing, and a native traversal query engine.

---

## Features

- **Fast In-Memory Graph**: Directed graph supporting typed nodes and edges with arbitrary JSON key-value properties.
- **ACID-inspired Durability**:
  - **Write-Ahead Log (WAL)**: All node and edge mutations are logged sequentially before committing to memory.
  - **Snapshot Checkpointing**: Periodic or shutdown checkpoints compact the graph into binary snapshot files (`nodes.chk`, `edges.chk`).
  - **Crash Recovery**: Automatic WAL replay on startup if an ungraceful shutdown occurs.
- **Graph Traversal & Search**:
  - Breadth-First Search (BFS) with depth constraints
  - Shortest path finding
  - Direct neighbor lookups
  - Filtering by node labels and edge types
- **Interactive CLI**: Interactive shell with command assistance, syntax validation, and formatted path outputs.

---

## Requirements

- **Java 21+** (OpenJDK 21 recommended)
- Maven Wrapper (`./mvnw`) is included in the project.

---

## Getting Started

### 1. Build

Compile and package into an executable standalone JAR:

```bash
./mvnw clean package
```

### 2. Run Interactive Shell

Run the CLI directly or point to a database name:

```bash
# Choose or create a database interactively:
java -jar target/bota4J-1.0-SNAPSHOT.jar

# Or specify a database name directly:
java -jar target/bota4J-1.0-SNAPSHOT.jar mydb
```

---

## Query Language Reference

### 1. Create Nodes
```sql
CREATE NODE <Label> [{key: value, ...}]
```
*Examples:*
```sql
CREATE NODE Person {"name": "Alice", "city": "Bengaluru"}
CREATE NODE Person {name: "Bob", age: 28}
CREATE NODE Company {name: "TechCorp"}
```

### 2. Create Edges
```sql
CREATE EDGE <Type> FROM <sourceUUID> TO <targetUUID> [{key: value, ...}]
```
*Example:*
```sql
CREATE EDGE KNOWS FROM 77732e09-9d3b-4cac-99d6-60fa98ce7967 TO 6a96d19e-b7e9-4534-91cd-35d1b6bc5d81 {"since": 2022}
```

### 3. Traversals & Search
```sql
-- Breadth-First Search with Depth
FIND NODES FROM <sourceUUID> DEPTH 2

-- BFS with Filters
FIND NODES FROM <sourceUUID> DEPTH 2 WHERE NODE_LABEL Person AND EDGE_LABEL KNOWS

-- Find Immediate Neighbours
FIND NEIGHBOURS OF <sourceUUID>
FIND NEIGHBOURS OF <sourceUUID> WHERE EDGE_LABEL KNOWS

-- Shortest Path
FIND PATH FROM <sourceUUID> TO <targetUUID>
FIND PATH FROM <sourceUUID> TO <targetUUID> WHERE EDGE_LABEL KNOWS
```

### 4. Inspection & Deletion
```sql
-- View database contents
SHOW GRAPH
SHOW NODES
SHOW EDGES

-- Delete entities
DELETE NODE <UUID>
DELETE EDGE <UUID>

-- Help & Exit
HELP
END
```

---

## Project Structure

```
├── pom.xml
├── mvnw / mvnw.cmd
└── src
    ├── main/java/com/project
    │   ├── Main.java                # CLI entry point
    │   ├── config/db/Config.java    # Database path and configuration
    │   ├── core/                    # Graph, Node, Edge data structures
    │   ├── query/                   # Query tokenizer, parser, and execution engine
    │   ├── server/BootManager.java  # Recovery and snapshot loader
    │   ├── storage/                 # Storage engine, WAL, and checkpoint managers
    │   └── traversal/               # BFS, Shortest Path, and filters
    └── test/java/com/project        # Unit and integration test suite
```

---

## Running Tests

Run the full automated test suite:

```bash
./mvnw test
```
