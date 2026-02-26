package com.project.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Setter
public class Edge {
    private final UUID id;
    private final String type;
    private final UUID source;
    private final UUID target;
    private Map<String, Object> properties;

    public Edge(UUID id, String type, UUID source, UUID target, Map<String, Object> properties) {
        this.id = id;
        this.type = type;
        this.source = source;
        this.target = target;
        this.properties = properties;
    }

    public Edge(String type, UUID source, UUID target) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.source = source;
        this.target = target;
        this.properties = new HashMap<>();
    }

    @Override
    public String toString() {
        return "Edge{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", fromNode=" + source +
                ", toNode=" + target +
                ", properties=" + properties +
                '}';
    }
}
