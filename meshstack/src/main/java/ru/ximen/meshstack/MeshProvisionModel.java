package ru.ximen.meshstack;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import static ru.ximen.meshstack.MeshBluetoothService.EXTRA_DATA;

/**
 * Created by ximen on 18.03.18.
 */

public class MeshProvisionModel {
    private final MeshStackService mContext;
    private final MeshProxyModel mProxy;

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
    private String mOOBKey;
    private List<Byte> confirmationInputs;
    private byte[] peerRandom;
    private byte[] peerConfirmation;
    private short peerAddress;
    private final MeshNetwork mNetwork;
    private BluetoothDevice mDevice;

    private KeyPair pair;
    private PublicKey peerPKey;
    private byte[] secret;
    private byte[] mConfirmationSalt;
    private byte[] provisionSalt;
    private byte[] mRandomBytes;
    private byte[] mConfirmationKey;
    private byte[] mConfirmation = new byte[16];
    private byte[] mAuthValue = new byte[16];

    public interface MeshProvisionFinishedOOBCallback {
        void finished(MeshDevice device, MeshNetwork network);
    }

    public interface MeshProvisionGetOOBCallback {
        void getOOB(MeshProvisionOOBCallback callback);
    }

    public interface MeshProvisionOOBCallback {
        void gotOOB(String oob);
    }

    public MeshProvisionModel(MeshStackService context) {
        mNetwork = context.getNetworkManager().getCurrentNetwork();
        mContext = context;
        mProxy = mContext.getProxy();
        pair = MeshEC.generatePair();
        confirmationInputs = new ArrayList<>();
        mContext.getMeshBluetoothService().registerCallback(MeshBluetoothService.MESH_PROVISION_DATA_OUT, characteristicCallback);
    }

    private MeshBluetoothService.MeshCharacteristicChangedCallback characteristicCallback = new MeshBluetoothService.MeshCharacteristicChangedCallback(){
        @Override
        public void onCharacteristicChanged(byte[] data) {
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
                peerPKey = MeshEC.getPeerPKey(pdu.getPKeyX(), pdu.getPKeyY());
                addInput(pdu.provisionData());
                secret = MeshEC.calculateSecret(pair, peerPKey);
                confirmation();
            } else if (pdu.getType() == MeshProvisionPDU.CONFIRMATION) {
                Log.d(TAG, "Got Confirmation");
                peerConfirmation = pdu.getConfirmation();
                sendRandom();
            } else if (pdu.getType() == MeshProvisionPDU.RANDOM) {
                Log.d(TAG, "Got Random");
                peerRandom = pdu.getRandom();
                if (peerConfirmation != remoteConfirmation(peerRandom)) {
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
        MeshDevice device = new MeshDevice(mDevice, peerAddress, MeshEC.getDeviceKey(secret, provisionSalt));
        Log.d(TAG, "Device key: " + Utils.toHexString(device.getDeviceKey()));
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
        pdu.setData(getProvisionData(data, peerRandom));
        mProxy.send(pdu);
    }

    private void sendRandom() {
        MeshProvisionPDU pdu = new MeshProvisionPDU(MeshProvisionPDU.RANDOM);
        pdu.setRandom(mRandomBytes);
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
        pdu.setPKeyX(MeshEC.getPKeyX(pair));
        pdu.setPKeyY(MeshEC.getPKeyY(pair));
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
        pdu.setConfirmation(getConfirmation(inputBytes, authValue));
        mProxy.send(pdu);
    }

    private void cancel() {
        Log.d(TAG, "Provision canceled");
        // tide up to initial state
    }

    private void addInput(byte[] data) {
        for (int i = 0; i < data.length; i++) confirmationInputs.add(data[i]);
    }

    public byte[] getConfirmation(byte[] inputs, byte[] authValue) {
        mAuthValue = authValue;
        byte[] mConfirmationSalt = MeshEC.s1(inputs);
        //Log.d(TAG, "Confirmation salt: " + Utils.toHexString(mConfirmationSalt));
        byte[] mConfirmationKey = MeshEC.k1(secret, mConfirmationSalt, "prck".getBytes());
        //Log.d(TAG, "Confirmation key: " + Utils.toHexString(mConfirmationSalt));
        SecureRandom random = new SecureRandom();
        mRandomBytes = new byte[16]; // 128 bits are converted to 16 bytes;
        random.nextBytes(mRandomBytes);
        //Log.d(TAG, "Random: " + Utils.toHexString(mRandomBytes));
        byte[] randomAuth = new byte[32];
        System.arraycopy(mRandomBytes, 0, randomAuth, 0, 16);
        System.arraycopy(authValue, 0, randomAuth, 16, 16);
        //Log.d(TAG, "Random||AuthValue: " + Utils.toHexString(randomAuth));

        mConfirmation = MeshEC.AES_CMAC(randomAuth, mConfirmationKey);
        //Log.d(TAG, "Confirmation: " + Utils.toHexString(mConfirmation));
        return mConfirmation;
    }

    public byte[] remoteConfirmation(byte[] randomBytes) {
        byte[] randomAuth = new byte[32];
        System.arraycopy(randomBytes, 0, randomAuth, 0, 16);
        System.arraycopy(mAuthValue, 0, randomAuth, 16, 16);
        //Log.d(TAG, "Remote Random||AuthValue: " + Utils.toHexString(randomAuth));
        byte[] confirmation = new byte[16];

        MeshEC.AES_CMAC(randomAuth, mConfirmationKey);
        //Log.d(TAG, "Remote confirmation: " + Utils.toHexString(confirmation));
        return confirmation;
    }

    public byte[] getProvisionData(byte[] data, byte[] peerRandom) {
        byte[] saltData = new byte[48];
        System.arraycopy(mConfirmationSalt, 0, saltData, 0, 16);
        System.arraycopy(mRandomBytes, 0, saltData, 16, 16);
        System.arraycopy(peerRandom, 0, saltData, 32, 16);
        //Log.d(TAG, " > ProvisionInputs: " + Utils.toHexString(saltData));
        provisionSalt = MeshEC.s1(saltData);
        //Log.d(TAG, " > ProvisionSalt: " + Utils.toHexString(provisionSalt));
        byte[] sessionKey = MeshEC.k1(secret, provisionSalt, "prsk".getBytes());
        //Log.d(TAG, " > SessionKey: " + Utils.toHexString(sessionKey));
        byte[] sessionNonce = new byte[13];
        System.arraycopy(MeshEC.k1(secret, provisionSalt, "prsn".getBytes()), 3, sessionNonce, 0, 13);
        //Log.d(TAG, " > Nonce: " + Utils.toHexString(sessionNonce));
        //Log.d(TAG, " > ProvisionData: " + Utils.toHexString(data));

        Pair<byte[], byte[]> t = MeshEC.AES_CCM(sessionKey, sessionNonce, data, 64);
        byte[] out = new byte[25 + 8];
        System.arraycopy(t.first, 0, out, 0, 25);
        System.arraycopy(t.second, 0, out, t.first.length, 8);
        return out;
    }
}

