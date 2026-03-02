package com.project.storage.io;

import com.project.core.Node;
import com.project.core.NodeRecord;
import com.project.storage.wal.Operations;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NodeRecordWriter implements AutoCloseable{
    private final DataOutputStream out;


    // [OP, NODEID, LABELLEN, LABEL, PROPLEN, PROPS] -- this is the format stored in nodes.chk

    public NodeRecordWriter(String filePath) throws FileNotFoundException {
        File file = new File(filePath);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        FileOutputStream fileOutputStream = new FileOutputStream(file, true);
        this.out = new DataOutputStream(new BufferedOutputStream(fileOutputStream));
    }

    public synchronized void writeCreate(UUID nodeId, String label, Map<String, Object> props) throws IOException {
        out.writeByte(Operations.ADD_NODE.getCode());
        out.writeLong(nodeId.getMostSignificantBits());
        out.writeLong(nodeId.getLeastSignificantBits());
        writeString(label);
        writeProperties(props);
        out.flush();
    }

    public synchronized void writeUpdate(UUID nodeId, String label, Map<String, Object> props) throws IOException {
        out.writeByte(Operations.UPDATE_NODE.getCode());
        out.writeLong(nodeId.getMostSignificantBits());
        out.writeLong(nodeId.getLeastSignificantBits());
        writeString(label);
        writeProperties(props);
        out.flush();
    }

    public synchronized void writeDelete(UUID nodeId) throws IOException {
        out.writeByte(Operations.DELETE_NODE.getCode());
        out.writeLong(nodeId.getMostSignificantBits());
        out.writeLong(nodeId.getLeastSignificantBits());
        out.flush();
    }

    private void writeString(String st) throws IOException {
        byte[] bytes = st.getBytes();
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private void writeProperties(Map<String, Object> props) throws IOException {
        out.writeInt(props.size());
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            writeString(entry.getKey());
            writeString(entry.getValue().toString());
        }
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public String readString(DataInputStream in) throws IOException {
        int len = in.readInt();
        byte[] stream = new byte[len];
        in.readFully(stream);
        return new String(stream, StandardCharsets.UTF_8);
    }

    public NodeRecord deserializeNode(DataInputStream in) throws IOException {

        // get the operation code
        byte opByte = in.readByte();
        Operations op = Operations.fromByte(opByte);

        // read id
        long msb = in.readLong();
        long lsb = in.readLong();
        UUID id = new UUID(msb, lsb);

        // if it is a delete operation
        if (op == Operations.DELETE_NODE) {
            // tombstone node
            Node node = new Node(id, null, null);
            return new NodeRecord(Operations.DELETE_NODE, node);
        }

        // node type
        String label = readString(in);

        // properties
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
