package com.project.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Setter
public class Node {
    private UUID id;
    private String label;
    private Map<String, Object> properties;

    public Node(UUID id, String label, Map<String, Object> properties) {
        this.id = id;
        this.label = label;
        this.properties = properties;
    }

    public Node(String label, Map<String, Object> properties) {
        id = UUID.randomUUID();
        this.label = label;
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", properties=" + properties +
                '}';
    }
}
