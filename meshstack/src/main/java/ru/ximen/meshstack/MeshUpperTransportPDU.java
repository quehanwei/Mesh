package ru.ximen.meshstack;

public class MeshUpperTransportPDU extends MeshPDU {
    private byte[] mData;
    private int SEQ;
    private boolean AKF;
    private byte AID;
    private short DST;

    public MeshUpperTransportPDU(int SEQ, boolean AKF, byte AID, short DST) {
        this.SEQ = SEQ;
        this.AKF = AKF;
        this.AID = AID;
        this.DST = DST;
    }

    public MeshUpperTransportPDU(MeshTransportPDU pdu) {
        this.SEQ = pdu.getSEQ();
        this.AKF = pdu.getAKF();
        this.AID = pdu.getAID();
        this.DST = pdu.getDST();
        setData(pdu.getAccessData());
    }

    @Override
    public byte[] data() {
        return mData;
    }

    @Override
    public void setData(byte[] data) {
        if (data.length > 380) throw new IllegalArgumentException("PDU size exceeds 380 octets");
        mData = data;
    }

    public int getSEQ() {
        return SEQ;
    }

    public boolean getAKF() {
        return AKF;
    }

    public byte getAID() {
        return AID;
    }

    public short getDST() {
        return DST;
    }
}
