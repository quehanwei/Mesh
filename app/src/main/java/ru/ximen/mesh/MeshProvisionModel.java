package ru.ximen.mesh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.ximen.mesh.MeshService.EXTRA_DATA;

/**
 * Created by ximen on 18.03.18.
 */

public class MeshProvisionModel {
    private final Context mContext;
    private final MeshProxyModel mProxy;
    private LocalBroadcastManager mBroadcastManger;

    final static private String TAG = "MeshProvision";

    private byte mAlgorithm;
    private byte mPKeyType;
    private byte mStaticOOBType;
    private byte mOutputOOBSize;
    private byte mOutputOOBAction;
    private byte mInputOOBSize;
    private byte mInputOOBAction;
    private MeshProvisionCallback mOOBCallback;
    private String mOOBKey;
    private BigInteger mAuthValue;
    MeshEC ec;

    public interface MeshProvisionCallback {
        void getOOB(MeshOOBCallback oobCallback);
    }

    public interface MeshOOBCallback {
        void gotOOB(String oob);
    }


    public MeshProvisionModel(Context context, MeshProxyModel proxy) {
        mContext = context;
        mProxy = proxy;
        IntentFilter filter = new IntentFilter(MeshService.ACTION_PROVISION_DATA_AVAILABLE);
        mBroadcastManger = LocalBroadcastManager.getInstance(mContext);
        mBroadcastManger.registerReceiver(mGattUpdateReceiver, filter);
        ec = new MeshEC();
        mAuthValue = new BigInteger("0");
        //String test = "test";
        //Log.d(TAG, Arrays.toString(ec.s1(test.getBytes())));

    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "Got provision data");
            byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
            MeshProvisionPDU pdu = new MeshProvisionPDU(data);
            if (pdu.getType() == MeshProvisionPDU.CAPABILITIES) {
                Log.d(TAG, "Got capabilities");
                mAlgorithm = pdu.getAlgorithms();
                mPKeyType = pdu.getPKeyType();
                mStaticOOBType = pdu.getStaticOOBType();
                mOutputOOBSize = pdu.getOutputOOBSize();
                mOutputOOBAction = pdu.getOutputOOBAction();
                mInputOOBSize = pdu.getInputOOBSize();
                mInputOOBAction = pdu.getInputOOBAction();
                BigInteger provisionData = new BigInteger(pdu.provisionData());
                mAuthValue = mAuthValue.or(provisionData);
                checkCapabilities();
            } else if (pdu.getType() == MeshProvisionPDU.PKEY) {
                Log.d(TAG, "Got Public key");
                ec.setPeerPKey(pdu.getPKeyX(), pdu.getPKeyY());

            } else if (pdu.getType() == MeshProvisionPDU.FAILED) {
                Log.e(TAG, "Got error PDU. Reason: " + pdu.errorString());
            }
            //cancel();
        }
    };

    public void startProvision(MeshProvisionCallback provisionCallback) {
        Log.d(TAG, "Sending invite PDU");
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.INVITE);
        mProxy.send(pdu);
        BigInteger provisionData = new BigInteger(pdu.provisionData());
        mAuthValue = mAuthValue.or(provisionData);
        mOOBCallback = provisionCallback;
    }

    public void close() {
        mBroadcastManger.unregisterReceiver(mGattUpdateReceiver);
    }

    private void checkCapabilities() {
        Log.d(TAG, "Checking capabilities");
        if (mAlgorithm != 1) {      // Bit 0
            Log.d(TAG, "Device algorithm " + mAlgorithm + " not supported");
            cancel();
        } else Log.d(TAG, "Algorithm FIPS P-256 EC");
        if (mPKeyType > 1) {        // Bit 0
            Log.e(TAG, "Device public key type value " + mPKeyType + " prohibited");
            cancel();
        } else if (mPKeyType == 1) Log.d(TAG, "Public key OOB information available");
        else Log.d(TAG, "Public key OOB information not available");
        if (mStaticOOBType > 1) {
            Log.e(TAG, "Device static OOB information value " + mStaticOOBType + " prohibited");
            cancel();
        } else if (mStaticOOBType == 1) Log.d(TAG, "Static OOB information available");
        else Log.d(TAG, "Static OOB information not available");
        if (mOutputOOBSize > 8) {
            Log.e(TAG, "Device OOB size " + mOutputOOBSize + " not supported");
            cancel();
        } else if (mOutputOOBSize == 0) Log.d(TAG, "Device does not support OOB");
        else Log.d(TAG, "Device OOB size = " + mOutputOOBSize);
        if (mOutputOOBAction > 0x1f) {      // 5 bits only
            Log.e(TAG, "Device OOB action " + mOutputOOBAction + " not supported");
            cancel();
        } else {
            List<String> action = new ArrayList<>();
            if ((mOutputOOBAction & 1) != 0) action.add("Blink");
            if ((mOutputOOBAction & 2) != 0) action.add("Beep");
            if ((mOutputOOBAction & 4) != 0) action.add("Vibrate");
            if ((mOutputOOBAction & 8) != 0) action.add("Output numeric");
            if ((mOutputOOBAction & 16) != 0) action.add("Output alphanumeric");
            Log.d(TAG, "Device OOB action: " + action);
        }

        if (mInputOOBSize > 9) {
            Log.e(TAG, "Device Input OOB size " + mInputOOBSize + " not supported");
            cancel();
        } else if (mInputOOBSize == 0) Log.d(TAG, "Device does not support Input OOB");
        else Log.d(TAG, "Device Input OOB size = " + mInputOOBSize);
        if (mInputOOBAction > 0x0f) {
            Log.e(TAG, "Device Input OOB action " + mInputOOBAction + " not supported");
            cancel();
        } else {
            List<String> action = new ArrayList<>();
            if ((mInputOOBAction & 1) != 0) action.add("Push");
            if ((mInputOOBAction & 2) != 0) action.add("Twist");
            if ((mInputOOBAction & 4) != 0) action.add("Input number");
            if ((mInputOOBAction & 8) != 0) action.add("Input alphanumeric");
            Log.d(TAG, "Device Input OOB action: " + action);
        }
        start();
        mOOBCallback.getOOB(new MeshOOBCallback() {
            @Override
            public void gotOOB(String oob) {
                Log.d(TAG, "Got OOB Key: " + oob);
                mOOBKey = oob;
                PKey();
                // inputComplete();     // for input oob only
            }
        });
    }

    private void start() {
        Log.d(TAG, "Sending START PDU");
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.START);
        if (mAlgorithm == 1) pdu.setAlgorithm((byte) 0);
        pdu.setPKeyType(mPKeyType);
        if (mStaticOOBType > 0) {
            pdu.setAuthMethod((byte) 1);                    // Static OOB auth used
            pdu.setAuthAction((byte) 0);                    // 5.4.1.3
            pdu.setAuthSize((byte) 0);                      // 5.4.1.3
        } else if (mOutputOOBSize > 0) {
            pdu.setAuthMethod((byte) 2);                    // Output OOB auth used
            if ((mOutputOOBAction & 8) != 0)
                pdu.setAuthAction((byte) 3);                 // Output number preferred
            else if ((mOutputOOBAction & 16) != 0)
                pdu.setAuthAction((byte) 4);               // Next Output alphanumeric
            else if ((mOutputOOBAction & 2) != 0)
                pdu.setAuthAction((byte) 1);                // Next Beep
            else if ((mOutputOOBAction & 1) != 0)
                pdu.setAuthAction((byte) 0);                // Next Blink
            else
                pdu.setAuthAction((byte) 2);                // Else vibrate
            pdu.setAuthSize(mOutputOOBSize);                // 5.4.1.3
        } else if (mInputOOBSize > 0) {
            pdu.setAuthMethod((byte) 3);                    // Input OOB auth used
            pdu.setAuthAction((byte) 2);                    // Default Input OOB action - input numeric (should take from settings)
            pdu.setAuthSize((byte) 1);                      // Default Input OOB size (should take from settings)
        } else {
            pdu.setAuthMethod((byte) 0);                    // No OOB auth used
            pdu.setAuthAction((byte) 0);                    // 5.4.1.3
            pdu.setAuthSize((byte) 0);                      // 5.4.1.3
        }
        Log.d(TAG, "START PDU: " + Arrays.toString(pdu.data()));
        BigInteger provisionData = new BigInteger(pdu.provisionData());
        mAuthValue = mAuthValue.or(provisionData);
        mProxy.send(pdu);
    }

    private void inputComplete() {
        Log.d(TAG, "Sending Input Complete PDU");
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.INPUT_COMPLETE);
        mProxy.send(pdu);
    }

    private void PKey() {
        Log.d(TAG, "Sending Public Key PDU");
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.PKEY);
        pdu.setPKeyX(ec.getPKeyX());
        pdu.setPKeyY(ec.getPKeyY());
        mAuthValue = mAuthValue.or(ec.getPKeyX());
        mAuthValue = mAuthValue.or(ec.getPKeyY());
        mProxy.send(pdu);
    }

    private void confirmation() {
        Log.d(TAG, "Sending Confirmation PDU");
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.CONFIRMATION);
        byte[] authValue = new byte[16];
        if ((mOutputOOBAction & 16) != 0) {
            // alphanumeric
            byte[] bytes = mOOBKey.getBytes();
            System.arraycopy(bytes, 0, authValue, 0, bytes.length);
        } else {
            byte[] bytes = ByteBuffer.allocate(mInputOOBSize).putInt(Integer.parseInt(mOOBKey)).array();
            System.arraycopy(bytes, 0, authValue, authValue.length - bytes.length, bytes.length);
        }
        BigInteger data = new BigInteger(authValue);
        mAuthValue = mAuthValue.or(data);
        byte[] confirmationSalt = ec.s1(mAuthValue.toByteArray());
        mProxy.send(pdu);
    }

    private void cancel() {
        Log.d(TAG, "Provision canceled");
        // tide up to initial state
    }
}

