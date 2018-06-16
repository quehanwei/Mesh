package ru.ximen.mesh;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
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
    private MeshProvisionFinishedOOBCallback mFinishedCallback;
    private MeshProvisionGetOOBCallback mGetOOBCallback;
    private MeshProvisionOOBCallback mOOBCallback;
    private String mOOBKey;
    private List<Byte> confirmationInputs;
    private byte[] peerRandom;
    private byte[] peerConfirmation;
    private short peerAddress;
    private final MeshNetwork mNetwork;
    private BluetoothDevice mDevice;
    MeshEC ec;


    public interface MeshProvisionFinishedOOBCallback {
        void finished(MeshDevice device, MeshNetwork network);
    }

    public interface MeshProvisionGetOOBCallback {
        void getOOB(MeshProvisionOOBCallback callback);
    }

    public interface MeshProvisionOOBCallback {
        void gotOOB(String oob);
    }

    public MeshProvisionModel(Context context, MeshProxyModel proxy, MeshNetwork network) {
        mNetwork = network;
        mContext = context;
        mProxy = proxy;
        IntentFilter filter = new IntentFilter(MeshService.ACTION_PROVISION_DATA_AVAILABLE);
        mBroadcastManger = LocalBroadcastManager.getInstance(mContext);
        mBroadcastManger.registerReceiver(mGattUpdateReceiver, filter);
        ec = new MeshEC();
        confirmationInputs = new ArrayList<>();
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
                addInput(pdu.provisionData());
                checkCapabilities();
            } else if (pdu.getType() == MeshProvisionPDU.PKEY) {
                Log.d(TAG, "Got Public key:");
                Log.d(TAG, "X: " + Utils.toHexString(pdu.getPKeyX()));
                Log.d(TAG, "Y: " + Utils.toHexString(pdu.getPKeyY()));
                ec.setPeerPKey(pdu.getPKeyX(), pdu.getPKeyY());
                addInput(pdu.provisionData());
                ec.calculateSecret();
                confirmation();
            } else if (pdu.getType() == MeshProvisionPDU.CONFIRMATION) {
                Log.d(TAG, "Got Confirmation");
                peerConfirmation = pdu.getConfirmation();
                sendRandom();
            } else if (pdu.getType() == MeshProvisionPDU.RANDOM) {
                Log.d(TAG, "Got Random");
                peerRandom = pdu.getRandom();
                if (peerConfirmation != ec.remoteConfirmation(peerRandom)) {
                    Log.e(TAG, "Confirmation doesn't match!");
                }
                sendData();
            } else if (pdu.getType() == MeshProvisionPDU.COMPLETE) {
                Log.d(TAG, "Got Provision complete");

                provisionComplete();
            } else if (pdu.getType() == MeshProvisionPDU.FAILED) {
                Log.e(TAG, "Got error PDU. Reason: " + pdu.errorString());
            }
            //cancel();
        }
    };

    private void provisionComplete() {
        MeshDevice device = new MeshDevice(mDevice, peerAddress, ec.getDeviceKey());
        device.setName(device.getMAC());
        mFinishedCallback.finished(device, mNetwork);
    }

    private void sendData() {
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.DATA);
        byte[] NetworkKey = mNetwork.getNetKey();
        Log.d(TAG, "Network key: " + Utils.toHexString(NetworkKey));
        short KeyIndex = mNetwork.getNetKeyIndex();
        byte Flags = 0;
        int IVIndex = mNetwork.getIVIndex();
        peerAddress = mNetwork.getNextUnicastAddress();
        Log.d(TAG, "Peer address: " + peerAddress);
        byte[] data = new byte[25];
        System.arraycopy(NetworkKey, 0, data, 0, 16);
        data[16] = (byte) (KeyIndex >>> 8);
        data[17] = (byte) (KeyIndex & 0x0ff);
        data[18] = Flags;
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(IVIndex);
        System.arraycopy(b.array(), 0, data, 19, 4);
        data[23] = (byte) (peerAddress >>> 8);
        data[24] = (byte) (peerAddress & 0x0ff);
        pdu.setData(ec.getProvisionData(data, peerRandom));
        mProxy.send(pdu);
    }

    private void sendRandom() {
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.RANDOM);
        pdu.setRandom(ec.getRandom());
        mProxy.send(pdu);
    }

    public void startProvision(BluetoothDevice device, MeshProvisionGetOOBCallback getOOB, MeshProvisionFinishedOOBCallback finished) {
        mDevice = device;
        mFinishedCallback = finished;
        mGetOOBCallback = getOOB;
        Log.d(TAG, "Invite PDU");
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.INVITE);
        mProxy.send(pdu);
        addInput(pdu.provisionData());
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
        mGetOOBCallback.getOOB(new MeshProvisionOOBCallback() {
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
        Log.d(TAG, "START PDU: " + Utils.toHexString(pdu.data(), " "));
        addInput(pdu.provisionData());
        mProxy.send(pdu);
    }

    private void inputComplete() {
        Log.d(TAG, "Input Complete PDU");
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.INPUT_COMPLETE);
        mProxy.send(pdu);
    }

    private void PKey() {
        Log.d(TAG, "Public Key PDU");
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.PKEY);
        pdu.setPKeyX(ec.getPKeyX());
        pdu.setPKeyY(ec.getPKeyY());
        addInput(pdu.provisionData());
        mProxy.send(pdu);
    }

    private void confirmation() {
        Log.d(TAG, "Confirmation PDU");
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.CONFIRMATION);

        Log.d(TAG, "Confirmation inputs: " + confirmationInputs.toString());
        byte[] authValue = new byte[16];
        if ((mOutputOOBAction & 16) != 0) {
            // alphanumeric
            byte[] bytes = mOOBKey.getBytes();
            System.arraycopy(bytes, 0, authValue, 0, bytes.length);
        } else {
            byte[] bytes = ByteBuffer.allocate(mOutputOOBSize).putInt(Integer.parseInt(mOOBKey)).array();
            System.arraycopy(bytes, 0, authValue, authValue.length - bytes.length, bytes.length);
        }
        Log.d(TAG, "Auth value: " + Utils.toHexString(authValue));
        byte[] inputBytes = new byte[confirmationInputs.size()];
        for (int index = 0; index < confirmationInputs.size(); index++) {
            inputBytes[index] = confirmationInputs.get(index);
        }
        pdu.setConfirmation(ec.getConfirmation(inputBytes, authValue));
        mProxy.send(pdu);
    }

    private void cancel() {
        Log.d(TAG, "Provision canceled");
        // tide up to initial state
    }

    private void addInput(byte[] data) {
        for (int i = 0; i < data.length; i++) confirmationInputs.add(data[i]);
    }
}

