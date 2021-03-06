package ru.ximen.meshstack;

import android.util.Log;

import org.spongycastle.crypto.InvalidCipherTextException;

import java.util.HashMap;
import java.util.Map;

public class MeshUpperTransportLayer {
    final static private String TAG = MeshUpperTransportLayer.class.getSimpleName();
    private MeshStackService mContext;
    private HashMap<String, MeshProcedure.MeshMessageCallback> callbackHashMap;

    public MeshUpperTransportLayer(MeshStackService context) {
        mContext = context;
        callbackHashMap = new HashMap<>();
    }

    private byte[] getOpcode(byte[] data) {
        byte[] result = null;
        if (((data[0] >>> 7) & 0x01) == 0) {           // Single octet
            result = new byte[1];
            result[0] = data[0];
        } else if (((data[0] >>> 6) & 0x03) == 2) { // Two octets
            result = new byte[2];
            result[0] = data[0];
            result[1] = data[1];
        } else if (((data[0] >>> 6) & 0x03) == 3) { // Three octets
            result = new byte[3];
            result[0] = data[0];
            result[1] = data[1];
            result[2] = data[2];
        }
        return result;
    }

    public static byte[] getNonce(MeshNetwork network, short SRC, short DST, int SEQ, boolean SZMIC, boolean af) {
        byte[] result = new byte[13];
        int ivi = network.getIVIndex();
        if (af) result[0] = 0x01;
        else result[0] = 0x02;         // Application or Device nonce
        result[1] = SZMIC ? (byte) 0x80 : 0;
        result[4] = (byte) (SEQ & 0xFF);
        result[3] = (byte) ((SEQ >>> 8) & 0xFF);
        result[2] = (byte) (SEQ >>> 16);
        result[6] = (byte) (SRC & 0xFF);
        result[5] = (byte) (SRC >>> 8);
        result[8] = (byte) (DST & 0xFF);
        result[7] = (byte) (DST >>> 8);
        result[12] = (byte) (ivi & 0xFF);
        result[11] = (byte) ((SEQ >>> 8) & 0xFF);
        result[10] = (byte) ((SEQ >>> 16) & 0xFF);
        result[9] = (byte) (SEQ >>> 24);
        return result;
    }

    public void registerCallback(byte[] opcode, short DST, MeshProcedure.MeshMessageCallback callback) {
        Log.d(TAG, "Registering callback for " + Utils.toHexString(opcode) + " and " + DST);
        callbackHashMap.put(callbackKey(opcode, DST), callback);
    }

    private String callbackKey(byte[] opcode, short DST) {
        byte[] key = new byte[opcode.length + 2];
        System.arraycopy(opcode, 0, key, 0, opcode.length);
        key[opcode.length] = (byte) (DST >>> 8);
        key[opcode.length + 1] = (byte) (DST & 0xff);
        Log.d(TAG, "Callback key " + Utils.toHexString(key));
        return Utils.toHexString(key);
    }

    public void newPDU(MeshUpperTransportPDU pdu, short addr, boolean SZMIC) {
        byte[] encData = pdu.data();
        if (pdu.getAKF()) {
            // Todo: AppKey
        } else {
            // DevKey
            byte[] key = mContext.getNetworkManager().getCurrentNetwork().getDeviceKey(addr);
            byte[] nonce = getNonce(mContext.getNetworkManager().getCurrentNetwork(), addr, mContext.getNetworkManager().getCurrentNetwork().getAddress(), pdu.getSEQ(), SZMIC, false);
            byte[] unencData = null;
            try {
                unencData = MeshEC.AES_CCM_Decrypt(key, nonce, encData, SZMIC ? 64 : 32);
            } catch (InvalidCipherTextException e) {
                e.printStackTrace();
            }
            //Log.d(TAG, "Decrypted Data: " + Utils.toHexString(unencData));
            byte[] opcode = getOpcode(unencData);
            //Log.d(TAG, "Opcode: " + Utils.toHexString(opcode));
            byte[] accessData = new byte[unencData.length - opcode.length];
            System.arraycopy(unencData, opcode.length, accessData, 0, accessData.length);
            //Log.d(TAG, "Data: " + Utils.toHexString(accessData));

            MeshProcedure.MeshMessageCallback callback = callbackHashMap.get(callbackKey(opcode, addr));

            for (Map.Entry<String, MeshProcedure.MeshMessageCallback> entry : callbackHashMap.entrySet()) {
                String tkey = entry.getKey();
                MeshProcedure.MeshMessageCallback value = entry.getValue();
                Log.d(TAG, tkey);
                Log.d(TAG, value.toString());
            }


            if (!(null == callback)) {
                MeshStatusResult result = new MeshStatusResult();
                result.setData(accessData);
                callback.status(result);
            } else {
                Log.d(TAG, "Callback not fount for " + Utils.toHexString(opcode) + " and " + addr);
            }
        }
    }
}
