package com.project.storage.wal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class WALWriter implements AutoCloseable {
    private DataOutputStream out;
    private final String walPath;
    private final Object WalLock = new Object();
    private volatile boolean writePaused = false;

    public WALWriter(String filename) throws FileNotFoundException {
        File file = new File(filename);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        this.walPath = file.getAbsolutePath();
        this.out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, true)));
    }

    public File getWalFile() {
        return new File(walPath);
    }

    public void logNodeOperations(Operations op, UUID id, String label, Map<String, Object> props) throws IOException, InterruptedException {
        synchronized (WalLock) {
            while (writePaused) {
                WalLock.wait();
            }

            out.writeByte(op.getCode());
            out.writeLong(id.getMostSignificantBits());
            out.writeLong(id.getLeastSignificantBits());

            if (op != Operations.DELETE_NODE) {
                writeString(label != null ? label : "");
                writeProperties(props);
            }

            out.flush();
        }
    }

    public void closeCurrentStream() throws IOException {
        synchronized (WalLock) {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }

    public void logEdgeOperations(Operations op, UUID id, UUID sourceId, UUID targetId, String type, Map<String, Object> props) throws IOException, InterruptedException {
        synchronized (WalLock) {
            while (writePaused) {
                WalLock.wait();
            }

            out.writeByte(op.getCode());
            out.writeLong(id.getMostSignificantBits());
            out.writeLong(id.getLeastSignificantBits());

            if (op != Operations.DELETE_EDGE) {
                out.writeLong(sourceId.getMostSignificantBits());
                out.writeLong(sourceId.getLeastSignificantBits());

                out.writeLong(targetId.getMostSignificantBits());
                out.writeLong(targetId.getLeastSignificantBits());

                writeString(type != null ? type : "");
                writeProperties(props);
            }

            out.flush();
        }
    }

    private void writeString(String st) throws IOException {
        if (st == null) {
            out.writeInt(0);
            return;
        }
        byte[] bytes = st.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private void writeProperties(Map<String, Object> props) throws IOException {
        if (props == null) {
            out.writeInt(0);
            return;
        }
        out.writeInt(props.size());
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            writeString(entry.getKey());
            writeString(entry.getValue() != null ? entry.getValue().toString() : "");
        }
    }

    public void switchTo(File filename) throws IOException {
        synchronized (WalLock) {
            if (out != null) {
                out.flush();
                out.close();
            }
            File parent = filename.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            this.out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename, true)));
        }
    }

    public void truncate() throws IOException {
        synchronized (WalLock) {
            if (out != null) {
                out.flush();
                out.close();
            }

            new FileOutputStream(walPath, false).close();
            this.out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(walPath, true)));
        }
    }

    @Override
    public void close() throws Exception {
        synchronized (WalLock) {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        }
    }

    public void pauseWrites() {
        synchronized (WalLock) {
            writePaused = true;
        }
    }

    public void resumeWrites() {
        synchronized (WalLock) {
            writePaused = false;
            WalLock.notifyAll();
        }
    }
}
