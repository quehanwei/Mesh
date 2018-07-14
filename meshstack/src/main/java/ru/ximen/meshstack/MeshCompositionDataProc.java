package ru.ximen.meshstack;

public class MeshCompositionDataProc extends MeshProcedure {
    protected byte[] get_opcode = {(byte) 0x80, 0x08};
    protected byte[] status_opcode = {(byte) 0x02};

    public MeshCompositionDataProc(MeshModel context) {
        super(context, "CompositionData");
    }

    public void get(byte page) {
        byte[] data = new byte[1];
        data[0] = page;
        send(get_opcode, data);
    }

    public void setStatusListner(MeshMessageCallback statusCallback) {
        mContext.getApplicationContext().getUpperTransportLayer().registerCallback(status_opcode, mContext.getDestination(), statusCallback);
    }


}
