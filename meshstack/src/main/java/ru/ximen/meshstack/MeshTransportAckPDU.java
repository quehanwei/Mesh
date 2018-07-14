package ru.ximen.meshstack;

public class MeshTransportAckPDU extends MeshPDU {
    private short SeqZero;
    private byte[] mData;

    public MeshTransportAckPDU(short seqZero, int blockAck) {
        SeqZero = seqZero;
        mData = new byte[4];
        mData[0] = (byte) ((blockAck >>> 24) & 0xff);
        mData[1] = (byte) ((blockAck >>> 16) & 0xff);
        mData[2] = (byte) ((blockAck >>> 8) & 0xff);
        mData[3] = (byte) (blockAck & 0xff);
    }

    @Override
    public byte[] data() {
        byte[] result = new byte[7];
        result[0] = 0;
        result[1] = (byte) ((SeqZero >>> 6) & 0x7f);
        result[2] = (byte) (SeqZero << 2);
        result[3] = mData[0];
        result[4] = mData[1];
        result[5] = mData[2];
        result[6] = mData[3];
        return result;
    }

    @Override
    public void setData(byte[] data) {
        mData = data;
    }
}
