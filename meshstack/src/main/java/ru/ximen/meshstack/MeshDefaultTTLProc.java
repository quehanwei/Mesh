package ru.ximen.meshstack;

public class MeshDefaultTTLProc extends MeshGSSProc {
    private final byte[] get_opcode = {(byte) 0x80, 0x0c};
    private final byte[] set_opcode = {(byte) 0x80, 0x0d};
    private final byte[] status_opcode = {(byte) 0x80, 0x0e};

    public MeshDefaultTTLProc(MeshModel context) {
        super(context, "DefaultTTL");
        super.get_opcode = this.get_opcode;
        super.set_opcode = this.set_opcode;
        super.status_opcode = this.status_opcode;
    }

    public void get() {
        super.get(new byte[0]);
    }

}


