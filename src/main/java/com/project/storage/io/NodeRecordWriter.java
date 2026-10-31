package com.project.storage.io;

import com.project.core.Node;
import com.project.core.NodeRecord;
import com.project.storage.wal.Operations;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NodeRecordWriter implements AutoCloseable {
    private final String filePath;
    private DataOutputStream out;

    public NodeRecordWriter(String filePath) throws FileNotFoundException {
        this.filePath = filePath;
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
        this.out = new DataOutputStream(new BufferedOutputStream(fileOutputStream));
    }

    public synchronized void writeCreate(UUID nodeId, String label, Map<String, Object> props) throws IOException {
        serializeNode(out, Operations.ADD_NODE, nodeId, label, props);
        out.flush();
    }

    public synchronized void writeUpdate(UUID nodeId, String label, Map<String, Object> props) throws IOException {
        serializeNode(out, Operations.UPDATE_NODE, nodeId, label, props);
        out.flush();
    }

    public synchronized void writeDelete(UUID nodeId) throws IOException {
        out.writeByte(Operations.DELETE_NODE.getCode());
        out.writeLong(nodeId.getMostSignificantBits());
        out.writeLong(nodeId.getLeastSignificantBits());
        out.flush();
    }

    public void serializeNode(DataOutputStream dos, Operations op, UUID nodeId, String label, Map<String, Object> props) throws IOException {
        dos.writeByte(op.getCode());
        dos.writeLong(nodeId.getMostSignificantBits());
        dos.writeLong(nodeId.getLeastSignificantBits());
        if (op != Operations.DELETE_NODE) {
            writeString(dos, label != null ? label : "");
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

    public NodeRecord deserializeNode(DataInputStream in) throws IOException {
        byte opByte = in.readByte();
        Operations op = Operations.fromByte(opByte);

        long msb = in.readLong();
        long lsb = in.readLong();
        UUID id = new UUID(msb, lsb);

        if (op == Operations.DELETE_NODE) {
            Node node = new Node(id, null, null);
            return new NodeRecord(Operations.DELETE_NODE, node);
        }

        String label = readString(in);

        int count = in.readInt();
        Map<String, Object> props = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String key = readString(in);
            String value = readString(in);
            props.put(key, value);
        }

        Node node = new Node(id, label, props);
        return new NodeRecord(op, node);
    }
}
