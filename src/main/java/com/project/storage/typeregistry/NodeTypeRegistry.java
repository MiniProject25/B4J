package com.project.storage.typeregistry;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class NodeTypeRegistry {
    private final Map<String, Integer> typeToId = new HashMap<>();
    private final Map<Integer, String> idToType = new HashMap<>();
    private int nextId = 1;

    private final String filepath;

    public NodeTypeRegistry(String dbPath) {
        this.filepath = dbPath + "/node_types.dat";
    }

    // load from disk at startup
    public void load() {
        File f = new File(filepath);
        if (!f.exists()) return;

        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            nextId = dis.readInt(); // read the int of the latest label in file
            int count = dis.readInt();

            for (int i = 0; i < count; i++) {
                int label_id = dis.readInt();
                String type = dis.readUTF();
                typeToId.put(type, label_id);
                idToType.put(label_id, type);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void write() throws FileNotFoundException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filepath)))) {
            // [ID][LEN_OF_LABEL_MAP][LABELS]
            dos.writeInt(nextId);
            dos.writeInt(typeToId.size());

            for (var entry : typeToId.entrySet()) {
                dos.writeInt(entry.getValue()); // ID of the label
                dos.writeUTF(entry.getKey()); // The label
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // get existing or create new labels
    public synchronized int getOrCreate(String type) {
        return typeToId.computeIfAbsent(type, t-> {
            int id = nextId++;
            idToType.put(id, type);
            return id;
        });
    }

    public String getType(int id) {
        return idToType.get(id);
    }
}
