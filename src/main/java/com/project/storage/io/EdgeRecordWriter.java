package com.project.storage.io;

import com.project.core.Edge;
import com.project.core.EdgeRecord;
import com.project.storage.wal.Operations;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class    EdgeRecordWriter implements AutoCloseable{
    DataOutputStream out;

    // [OP, EDGEID, TYPELEN, TYPE, FROMID, TOID, PROPLEN, PROPS] -- Stored in this format in edges.chks

    public EdgeRecordWriter(String filepath) throws IOException {
        File file = new File(filepath);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        FileOutputStream fos = new FileOutputStream(file, true);
        out = new DataOutputStream(new BufferedOutputStream(fos));
    }

    public synchronized void writeCreate(UUID edgeId, String type, UUID fromId, UUID toId, Map<String, Object> props) throws IOException {
        out.writeByte(Operations.ADD_EDGE.getCode());
        out.writeLong(edgeId.getMostSignificantBits());
        out.writeLong(edgeId.getLeastSignificantBits());
        writeString(type);
        out.writeLong(fromId.getMostSignificantBits());
        out.writeLong(fromId.getLeastSignificantBits());
        out.writeLong(toId.getMostSignificantBits());
        out.writeLong(toId.getLeastSignificantBits());
        writeProperties(props);
        out.flush();
    }

    public synchronized void writeUpdate(UUID edgeId, String type, UUID fromId, UUID toId, Map<String, Object> props) throws IOException {
        out.writeByte(Operations.UPDATE_EDGE.getCode());
        out.writeLong(edgeId.getMostSignificantBits());
        out.writeLong(edgeId.getLeastSignificantBits());
        writeString(type);
        out.writeLong(fromId.getMostSignificantBits());
        out.writeLong(fromId.getLeastSignificantBits());
        out.writeLong(toId.getMostSignificantBits());
        out.writeLong(toId.getLeastSignificantBits());
        writeProperties(props);
        out.flush();
    }

    public synchronized void writeDelete(UUID edgeId) throws IOException {
        out.writeByte(Operations.DELETE_EDGE.getCode());
        out.writeLong(edgeId.getMostSignificantBits());
        out.writeLong(edgeId.getLeastSignificantBits());
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

    public EdgeRecord deserializeEdge(DataInputStream in) throws IOException {

        // get the operation code
        byte opByte = in.readByte();
        Operations op = Operations.fromByte(opByte);

        // read id
        long msb = in.readLong();
        long lsb = in.readLong();
        UUID id = new UUID(msb, lsb);

        // if it is a delete operation
        if (op == Operations.DELETE_EDGE) {
            // tombstone node
            Edge edge = new Edge(id, null, null, null, null);
            return new EdgeRecord(op, edge);
        }

        // edge type
        String type = readString(in);
        // source uuid
        long sourceMsb = in.readLong();
        long sourceLsb = in.readLong();
        UUID sourceId = new UUID(sourceMsb, sourceLsb);
        // destination uuid
        long destMsb = in.readLong();
        long destLsb = in.readLong();
        UUID destId = new UUID(destMsb, destLsb);

        // properties
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
