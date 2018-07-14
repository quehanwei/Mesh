package ru.ximen.meshstack;

public class MeshRelayProc extends MeshGSSProc {
    private final byte[] get_opcode = {(byte) 0x80, 0x26};
    private final byte[] set_opcode = {(byte) 0x80, 0x27};
    private final byte[] status_opcode = {(byte) 0x80, 0x28};

    public MeshRelayProc(MeshModel context) {
        super(context, "Relay");
        super.get_opcode = this.get_opcode;
        super.set_opcode = this.set_opcode;
        super.status_opcode = this.status_opcode;
    }

    public void get() {
        super.get(new byte[0]);
    }

}

