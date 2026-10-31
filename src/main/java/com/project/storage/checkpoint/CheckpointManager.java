package com.project.storage.checkpoint;

import com.project.core.EdgeRecord;
import com.project.core.NodeRecord;
import com.project.storage.wal.WALWriter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

public class CheckpointManager {

    private final SnapshotService snapshotService;
    private final WALWriter walWriter;

    public CheckpointManager(SnapshotService snapshotService, WALWriter walWriter) {
        this.snapshotService = snapshotService;
        this.walWriter = walWriter;
    }

    public synchronized void createCheckpoint(Map<UUID, NodeRecord> nodes, Map<UUID, EdgeRecord> edges) throws Exception {
        File oldWalFile = walWriter.getWalFile();
        File newWalFile = new File(oldWalFile.getParent(), "wal_tmp.chk");

        // 1. Switch active writes to a temporary WAL
        walWriter.switchTo(newWalFile);

        try {
            // 2. Persist the current snapshot of all nodes and edges
            snapshotService.writeSnapshot(nodes, edges);

            // 3. Pause incoming writes briefly to rotate files
            walWriter.pauseWrites();
            walWriter.closeCurrentStream();

            // 4. Overwrite wal.chk with wal_tmp.chk (which contains only writes that occurred during snapshot)
            Files.move(newWalFile.toPath(), oldWalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 5. Reopen wal.chk
            walWriter.switchTo(oldWalFile);
        } finally {
            // Always ensure writes resume
            walWriter.resumeWrites();
        }
    }
}
