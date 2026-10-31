package com.project.storage.io;

import com.project.core.Edge;
import com.project.core.EdgeRecord;
import com.project.storage.wal.Operations;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EdgeRecordWriter implements AutoCloseable {
    private final String filepath;
    private DataOutputStream out;

    public EdgeRecordWriter(String filepath) throws IOException {
        this.filepath = filepath;
        File file = new File(filepath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(file, true);
        out = new DataOutputStream(new BufferedOutputStream(fos));
    }

    public synchronized void writeCreate(UUID edgeId, String type, UUID fromId, UUID toId, Map<String, Object> props) throws IOException {
        serializeEdge(out, Operations.ADD_EDGE, edgeId, type, fromId, toId, props);
        out.flush();
    }

    public synchronized void writeUpdate(UUID edgeId, String type, UUID fromId, UUID toId, Map<String, Object> props) throws IOException {
        serializeEdge(out, Operations.UPDATE_EDGE, edgeId, type, fromId, toId, props);
        out.flush();
    }

    public synchronized void writeDelete(UUID edgeId) throws IOException {
        out.writeByte(Operations.DELETE_EDGE.getCode());
        out.writeLong(edgeId.getMostSignificantBits());
        out.writeLong(edgeId.getLeastSignificantBits());
        out.flush();
    }

    public void serializeEdge(DataOutputStream dos, Operations op, UUID edgeId, String type, UUID fromId, UUID toId, Map<String, Object> props) throws IOException {
        dos.writeByte(op.getCode());
        dos.writeLong(edgeId.getMostSignificantBits());
        dos.writeLong(edgeId.getLeastSignificantBits());
        if (op != Operations.DELETE_EDGE) {
            writeString(dos, type != null ? type : "");
            dos.writeLong(fromId.getMostSignificantBits());
            dos.writeLong(fromId.getLeastSignificantBits());
            dos.writeLong(toId.getMostSignificantBits());
            dos.writeLong(toId.getLeastSignificantBits());
            writeProperties(dos, props);
        }
    }

    public void writeString(DataOutputStream dos, String st) throws IOException {
        if (st == null) {
            dos.writeInt(0);
            return;
        }
        byte[] bytes = st.getBytes(StandardCharsets.UTF_8);
        dos.writeInt(bytes.length);
        dos.write(bytes);
    }

    public void writeProperties(DataOutputStream dos, Map<String, Object> props) throws IOException {
        if (props == null) {
            dos.writeInt(0);
            return;
        }
        dos.writeInt(props.size());
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            writeString(dos, entry.getKey());
            writeString(dos, entry.getValue() != null ? entry.getValue().toString() : "");
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (out != null) {
            out.flush();
            out.close();
        }
    }

    public String readString(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0) return "";
        byte[] stream = new byte[len];
        in.readFully(stream);
        return new String(stream, StandardCharsets.UTF_8);
    }

    public EdgeRecord deserializeEdge(DataInputStream in) throws IOException {
        byte opByte = in.readByte();
        Operations op = Operations.fromByte(opByte);

        long msb = in.readLong();
        long lsb = in.readLong();
        UUID id = new UUID(msb, lsb);

        if (op == Operations.DELETE_EDGE) {
            Edge edge = new Edge(id, null, null, null, null);
            return new EdgeRecord(op, edge);
        }

        String type = readString(in);

        long sourceMsb = in.readLong();
        long sourceLsb = in.readLong();
        UUID sourceId = new UUID(sourceMsb, sourceLsb);

        long destMsb = in.readLong();
        long destLsb = in.readLong();
        UUID destId = new UUID(destMsb, destLsb);

        int count = in.readInt();
        Map<String, Object> props = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String key = readString(in);
            String value = readString(in);
            props.put(key, value);
        }

        Edge edge = new Edge(id, type, sourceId, destId, props);
        return new EdgeRecord(op, edge);
    }
}
