package ru.ximen.meshstack;

public class MeshGATTProxyProc extends MeshGSSProc {
    private final byte[] get_opcode = {(byte) 0x80, 0x12};
    private final byte[] set_opcode = {(byte) 0x80, 0x13};
    private final byte[] status_opcode = {(byte) 0x80, 0x14};

    public void get() {
        super.get(new byte[0]);
    }

    public MeshGATTProxyProc(MeshConfigurationModel context) {
        super(context, "GATTProxy");
        super.get_opcode = this.get_opcode;
        super.set_opcode = this.set_opcode;
        super.status_opcode = this.status_opcode;
    }
}
