package com.project.storage.wal;

public enum Operations {
    ADD_NODE((byte)1),
    DELETE_NODE((byte)2),
    UPDATE_NODE((byte)3),
    ADD_EDGE((byte)4),
    DELETE_EDGE((byte)5),
    UPDATE_EDGE((byte)6);

    final byte code;

    Operations(byte b) {
        code = b;
    }

    public byte getCode() {
        return code;
    }

    public static Operations fromByte(byte b) {
        for (Operations op : Operations.values()) {
            if (op.getCode() == b) {
                return op;
            }
        }
        throw new IllegalArgumentException("Incorrect Operation");
    }
}
