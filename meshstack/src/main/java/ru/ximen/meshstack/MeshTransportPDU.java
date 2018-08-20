package ru.ximen.meshstack;

public class MeshTransportPDU extends MeshPDU {
    final static private String TAG = MeshTransportPDU.class.getSimpleName();
    private byte[] mData;
    private boolean SEG;
    private boolean AKF;
    private byte AID;
    private boolean SZMIC = false;
    private short SeqZero;
    private byte SegO;
    private byte SegN;
    private int SEQ;
    private short DST;

    // Constructor for segmented PDUs
    public MeshTransportPDU(int SEQ, boolean AKF, byte AID, short DST, short SeqZero, byte SegO, byte SegN, boolean SZMIC) {
        this.SEQ = SEQ;
        this.AKF = AKF;
        this.AID = AID;
        this.SeqZero = SeqZero;
        this.SegO = SegO;
        this.SegN = SegN;
        this.DST = DST;
        this.SEG = true;
        this.SZMIC = SZMIC;
    }

    // Constructor for unsegmented PDUs
    public MeshTransportPDU(int SEQ, boolean AKF, byte AID, short DST) {
        this.SEQ = SEQ;
        this.AKF = AKF;
        this.AID = AID;
        this.DST = DST;
        this.SEG = false;
    }

    // Constructor for binary representation
    public MeshTransportPDU(byte[] data, int SEQ) {
        this.SEQ = SEQ;
        SEG = ((data[0] >>> 7) & (0x01)) == 1;
        AKF = ((data[0] >>> 6) & 0x01) == 1;
        AID = (byte) (data[0] & 0x3f);
        if (SEG) {
            SZMIC = ((data[1] >>> 7) & 0x01) == 1;
            SeqZero = (short) (((data[1] & 0x7F) << 6) + (data[2] >>> 2));
            SegO = (byte) (((data[2] & 0x03) << 3) + (data[3] >>> 5));
            SegN = (byte) (data[3] & 0x1f);
            mData = new byte[data.length - 4];
            System.arraycopy(data, 4, mData, 0, mData.length);
        } else {
            mData = new byte[data.length - 1];
            System.arraycopy(data, 1, mData, 0, mData.length);
        }
    }

    @Override
    public void setData(byte[] data) {
        mData = data;
    }

    @Override
    public byte[] data() {
        if (!SEG) {
            byte[] result = new byte[mData.length + 1];
            result[0] = (byte) ((((byte) (AKF ? 1 : 0)) << 6) + AID);
            System.arraycopy(mData, 0, result, 1, mData.length);
            return result;
        } else {
            byte[] result = new byte[mData.length + 4];
            result[0] = (byte) ((((byte) (AKF ? 1 : 0)) << 6) + AID + 0x80);
            result[1] = (byte) (((SZMIC ? 1 : 0) << 7) + (SeqZero >>> 6));
            result[2] = (byte) ((SeqZero & 0x3F) + (SegO >>> 3));
            result[3] = (byte) ((SegO << 5) + SegN);
            System.arraycopy(mData, 0, result, 4, mData.length);
            return result;
        }
    }


    public boolean isComplete() {
        return !SEG;
    }

    public boolean isLast() {
        return SegO == SegN;
    }

    public int getSEQ() {
        return SEQ;
    }

    public short getDST() {
        return DST;
    }

    public boolean getAKF() {
        return AKF;
    }

    public byte getAID() {
        return AID;
    }

    public byte[] getAccessData() {
        return mData;
    }

    public boolean getSZMIC() {
        return SZMIC;
    }

    public byte getSegO() {
        return SegO;
    }

    public byte getSegN() {
        return SegN;
    }
}
