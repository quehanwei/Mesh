package ru.ximen.mesh;

import android.util.Log;
import android.util.Pair;

import java.nio.ByteBuffer;

public class MeshNetworkPDU extends MeshPDU {
    final static private String TAG = "MeshNetworkPDU";
    private byte IVI;
    private byte NID;
    private byte CTL;
    private byte TTL;
    private int SEQ;
    private short SRC;
    private short DST;
    private byte[] transportPDU;
    private byte[] NetMIC;
    private byte[] mNonce;
    private MeshNetwork mNetwork;
    private byte[] mObfuscatedData;

    public MeshNetworkPDU(MeshNetwork network, short SRC, short DST, byte CTL, byte TTL) {
        mNetwork = network;
        this.CTL = CTL;
        this.TTL = TTL;
        if (CTL == 0) NetMIC = new byte[4];
        else NetMIC = new byte[8];
        this.SEQ = network.getNextSeq();
        this.SRC = SRC;
        this.DST = DST;
        IVI = (byte) (network.getIVIndex() & 0x1);
        Log.d(TAG, "IVI: " + IVI);
        NID = network.getNID();
        Log.d(TAG, "NID: " + NID);

        mNonce = new byte[13];
        mNonce[0] = 0x00;
        mNonce[1] = (byte) ((CTL << 7) + TTL);
        mNonce[4] = (byte) (SEQ & 0xFF);
        mNonce[3] = (byte) ((SEQ >> 8) & 0xFF);
        mNonce[2] = (byte) (SEQ >> 16);
        mNonce[6] = (byte) (SRC & 0xFF);
        mNonce[5] = (byte) (SRC >> 8);
        mNonce[7] = 0x00;
        mNonce[8] = 0x00;
        System.arraycopy(ByteBuffer.allocate(4).putInt(network.getIVIndex()).array(), 0, mNonce, 9, 4);
        Log.d(TAG, "Network Nonce: " + Utils.toHexString(mNonce));
    }

    void setTransportPDU(byte[] pdu) {
        byte[] data = new byte[pdu.length + 2];
        data[1] = (byte) (DST & 0xFF);
        data[0] = (byte) (DST >> 8);
        System.arraycopy(pdu, 0, data, 2, pdu.length);

        Pair<byte[], byte[]> t = MeshEC.AES_CCM(mNetwork.getEncryptionKey(), mNonce, data, (CTL == 0) ? 32 : 64);
        transportPDU = t.first;
        NetMIC = t.second;

        byte[] PrivacyRandomData = new byte[transportPDU.length + NetMIC.length];
        System.arraycopy(transportPDU, 0, PrivacyRandomData, 0, transportPDU.length);
        System.arraycopy(NetMIC, 0, PrivacyRandomData, transportPDU.length, NetMIC.length);

        byte[] privacyRandom = new byte[7];
        System.arraycopy(PrivacyRandomData, 0, privacyRandom, 0, 7);

        byte[] PECBData = new byte[16];
        System.arraycopy(ByteBuffer.allocate(4).putInt(mNetwork.getIVIndex()).array(), 0, PECBData, 5, 4);
        System.arraycopy(privacyRandom, 0, PECBData, 9, 7);

        byte[] PECB = MeshEC.e(mNetwork.getPrivacyKey(), PECBData);

        mObfuscatedData = new byte[6];
        mObfuscatedData[0] = (byte) ((CTL << 7) + TTL | PECB[0]);
        mObfuscatedData[3] = (byte) ((SEQ & 0xFF) | PECB[3]);
        mObfuscatedData[2] = (byte) (((SEQ >> 8) & 0xFF) | PECB[2]);
        mObfuscatedData[1] = (byte) ((SEQ >> 16) | PECB[1]);
        mObfuscatedData[5] = (byte) ((SRC & 0xFF) | PECB[5]);
        mObfuscatedData[4] = (byte) ((SRC >> 8) | PECB[4]);
    }

    @Override
    public byte[] data() {
        byte[] data = new byte[1 + mObfuscatedData.length + transportPDU.length + NetMIC.length];
        data[0] = (byte) ((IVI << 7) + NID);
        System.arraycopy(mObfuscatedData, 0, data, 1, mObfuscatedData.length);
        System.arraycopy(transportPDU, 0, data, 1 + mObfuscatedData.length, transportPDU.length);
        System.arraycopy(NetMIC, 0, data, 1 + mObfuscatedData.length + transportPDU.length, NetMIC.length);
        return data;
    }
}
