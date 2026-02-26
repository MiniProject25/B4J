package com.project.core;

import com.project.storage.wal.Operations;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EdgeRecord {
    Operations op;
    Edge edge;
}
