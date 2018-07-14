package ru.ximen.meshstack;

public class MeshOnOffProc extends MeshGSSProc {
    private static final byte[] get_opcode = {(byte) 0x82, 0x01};
    private static final byte[] set_opcode = {(byte) 0x82, 0x02};
    private static final byte[] status_opcode = {(byte) 0x82, 0x03};

    public MeshOnOffProc(MeshModel context) {
        super(context, "OnOff");
    }

    public void get() {
        super.get(new byte[0]);
    }

    @Override
    public void set(byte[] data) {
        super.set(data);
    }
}
