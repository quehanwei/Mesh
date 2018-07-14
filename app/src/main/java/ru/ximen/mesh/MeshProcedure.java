package ru.ximen.mesh;

import android.util.Log;
import android.util.Pair;

public abstract class MeshProcedure {
    final static private String TAG = "MeshProcedure";
    protected String name;
    protected MeshModel mContext;
    private int SEQ;
    private boolean aif;
    private byte[] mAppKey;

    public interface MeshMessageCallback {
        void status(MeshStatusResult result);
    }

    public MeshProcedure(MeshModel context, String name) {
        this.mContext = context;
        this.name = name;
        aif = mContext.getAIF();
        mAppKey = mContext.getAppKey();
    }

    public String getName() {
        return name;
    }

    protected void send(byte[] opcode, byte[] data) throws IllegalArgumentException {
        if (data.length > 377)
            throw new IllegalArgumentException("Payload size exceeds 377 octets");
        byte[] unenc = new byte[opcode.length + data.length];
        System.arraycopy(opcode, 0, unenc, 0, opcode.length);
        if (data.length > 0) System.arraycopy(data, 0, unenc, opcode.length, data.length);
        //Log.d(TAG, "Unencrypted data: " + Utils.toHexString(unenc));
        SEQ = mContext.getNetwork().getNextSeq();
        //Log.d(TAG, "SEQ: " + SEQ);
        byte[] nonce = MeshUpperTransportLayer.getNonce(mContext.getNetwork(), mContext.getNetwork().getAddress(), mContext.getDestination(), SEQ, false, aif);
        //Log.d(TAG, "Nonce: " + Utils.toHexString(nonce));
        //Log.d(TAG, "Key: " + Utils.toHexString(mAppKey));
        Pair<byte[], byte[]> result = MeshEC.AES_CCM(mAppKey, nonce, unenc, 32);        // Ignoring SZMIC
        byte[] enc = new byte[result.first.length + result.second.length];
        System.arraycopy(result.first, 0, enc, 0, result.first.length);
        System.arraycopy(result.second, 0, enc, result.first.length, result.second.length);
        //Log.d(TAG, "Encrypted data: " + Utils.toHexString(enc));
        MeshUpperTransportPDU pdu = new MeshUpperTransportPDU(SEQ, aif, (byte) 0, mContext.getDestination());    // AID = 0 TODO: Application key management
        pdu.setData(enc);
        //Log.d(TAG, "Upper Transport PDU: " + Utils.toHexString(pdu.data()));
        mContext.getTransportLayer().send(pdu);
    }
}
