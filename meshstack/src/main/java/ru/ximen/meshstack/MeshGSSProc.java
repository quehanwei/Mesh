package ru.ximen.meshstack;

public abstract class MeshGSSProc extends MeshProcedure {
    protected byte[] get_opcode;
    protected byte[] set_opcode;
    protected byte[] status_opcode;

    public MeshGSSProc(MeshModel context, String name) {
        super(context, name);
    }

    protected void get(byte[] data) {
        send(get_opcode, data);
    }

    protected void set(byte[] data) {
        send(set_opcode, data);
    }

    public void setStatusListner(MeshMessageCallback statusCallback) {
        mContext.getStackService().getUpperTransportLayer().registerCallback(status_opcode, mContext.getDestination(), statusCallback);
    }
}
