package com.project.storage.checkpoint;

import com.project.core.EdgeRecord;
import com.project.core.NodeRecord;
import com.project.storage.wal.WALWriter;

import java.io.File;
import java.io.IOException;
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
        try {
                System.out.println("Creating wal_tmp.chk");

                File oldWalFile = walWriter.getWalFile();
                File newWalFile = new File(oldWalFile.getParent(), "wal_tmp.chk");

                // switching to the temp file
                walWriter.switchTo(newWalFile);

                System.out.println("Creating checkpoint");
                // write snapshot
                snapshotService.writeSnapshot(nodes, edges);

                walWriter.pauseWrites();

                walWriter.closeCurrentStream();

                // replace wal_tmp.chk with wal.chk
                Files.move(newWalFile.toPath(), oldWalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                walWriter.switchTo(oldWalFile);

                walWriter.resumeWrites();

                System.out.println("Checkpoint completed successfully!");
        }
        catch (Exception e) {
            System.out.println("Checkpointing failed: " + e.getMessage());
        }
    }
}
