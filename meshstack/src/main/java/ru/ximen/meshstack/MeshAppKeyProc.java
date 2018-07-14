package ru.ximen.meshstack;

public class MeshAppKeyProc extends MeshProcedure {
    protected byte[] add_opcode = {(byte) 0x00};
    protected byte[] del_opcode = {(byte) 0x80, 0x00};
    protected byte[] get_opcode = {(byte) 0x80, 0x01};
    protected byte[] list_opcode = {(byte) 0x80, 0x02};
    protected byte[] status_opcode = {(byte) 0x80, 0x03};
    protected byte[] update_opcode = {(byte) 0x01};

    public MeshAppKeyProc(MeshModel context) {
        super(context, "AppKey");
    }

    public void add(byte[] index, byte[] AppKey) {
        byte[] data = new byte[19];
        System.arraycopy(index, 0, data, 0, index.length);
        System.arraycopy(AppKey, 0, data, 3, AppKey.length);
        send(add_opcode, data);
    }

    public void update(byte[] index, byte[] AppKey) {
        byte[] data = new byte[19];
        System.arraycopy(index, 0, data, 0, index.length);
        System.arraycopy(AppKey, 0, data, 3, AppKey.length);
        send(update_opcode, data);
    }

    public void delete(byte[] index) {
        send(del_opcode, index);
    }

    public void get(byte[] index) {
        send(get_opcode, index);
    }

    public void setStatusListner(MeshMessageCallback statusCallback, MeshMessageCallback listCallback) {
        mContext.getApplicationContext().getUpperTransportLayer().registerCallback(status_opcode, mContext.getDestination(), statusCallback);
        mContext.getApplicationContext().getUpperTransportLayer().registerCallback(list_opcode, mContext.getDestination(), listCallback);
    }
}
