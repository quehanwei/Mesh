package ru.ximen.mesh;

import android.util.Log;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Created by ximen on 25.03.18.
 */

public class MeshProvisionPDU extends MeshPDU {
    private byte[] mData;

    final static public byte INVITE = 0x00;
    final static public byte CAPABILITIES = 0x01;
    final static public byte START = 0x02;
    final static public byte PKEY = 0x03;
    final static public byte INPUT_COMPLETE = 0x04;
    final static public byte CONFIRMATION = 0x05;
    final static public byte RANDOM = 0x06;
    final static public byte DATA = 0x07;
    final static public byte COMPLETE = 0x08;
    final static public byte FAILED = 0x09;
    final static private String TAG = "MeshProvisionPDU";
    private byte mType;

    public MeshProvisionPDU(byte type) {
        mType = type;
        switch (mType) {
            case INVITE:
                mData = new byte[2];
                mData[1] = 0;           // 5.4.1.1
                break;
            case START:
                mData = new byte[6];    // 5.4.1.3
                break;
            case PKEY:
                mData = new byte[65];    // 5.4.1.4
                break;
            case INPUT_COMPLETE:
                mData = new byte[1];    // 5.4.1.5
                break;
            case CONFIRMATION:
                mData = new byte[17];   // 5.4.1.6
                break;
            case RANDOM:
                mData = new byte[17];   // 5.4.1.6
                break;
            case DATA:
                mData = new byte[34];   // 5.4.1.6
                break;
        }
        mData[0] = mType;               // 5.4.1
    }

    public MeshProvisionPDU(byte[] data) {
        Log.d(TAG, "Reconstructing PDU from data " + Arrays.toString(data));
        mData = data;
        mType = mData[0];
    }

    public byte getType() {
        return mType;
    }

    public byte getElementsNumber() {
        return mData[1];
    }

    public byte getAlgorithms() {
        return mData[3];
    }

    public byte getPKeyType() {
        return mData[4];
    }

    public byte getStaticOOBType() {
        return mData[5];
    }

    public byte getOutputOOBSize() {
        return mData[6];
    }

    public byte getOutputOOBAction() {
        return mData[8];
    }

    public byte getInputOOBSize() {
        return mData[9];
    }

    public byte getInputOOBAction() {
        return mData[11];
    }

    public void setAlgorithm(byte algorithm) {
        if (mType == START) mData[1] = algorithm;
    }

    public void setPKeyType(byte pktype) {
        if (mType == START) mData[2] = pktype;
    }

    public void setAuthMethod(byte method) {
        if (mType == START) mData[3] = method;
    }

    public void setAuthAction(byte action) {
        if (mType == START) mData[4] = action;
    }

    public void setAuthSize(byte size) {
        if (mType == START) mData[5] = size;
    }

    public void setPKeyX(BigInteger x) {
        byte[] tx = x.toByteArray();
        if (tx[0] != 0x00) {
            System.arraycopy(tx, 0, mData, 1, 32);
        } else {
            System.arraycopy(tx, 1, mData, 1, 32);
        }
    }

    public BigInteger getPKeyX() {
        byte[] data = new byte[32];
        System.arraycopy(mData, 1, data, 0, 32);
        return new BigInteger(data);
    }

    public BigInteger getPKeyY() {
        byte[] data = new byte[32];
        System.arraycopy(mData, 33, data, 0, 32);
        return new BigInteger(data);
    }

    public void setPKeyY(BigInteger y) {
        byte[] ty = y.toByteArray();
        if (ty[0] != 0x00) {
            System.arraycopy(ty, 0, mData, 33, 32);
        } else {
            System.arraycopy(ty, 1, mData, 33, 32);
        }
    }

    public void setConfirmation(byte[] confirmation) {
        System.arraycopy(confirmation, 0, mData, 1, 16);
    }

    @Override
    public byte[] data() {
        return mData;
    }

    public byte[] provisionData() {
        byte[] data = new byte[mData.length - 1];
        System.arraycopy(mData, 1, data, 0, data.length);
        return data;
    }

    public byte errorCode() {
        if (mType == FAILED) {
            return mData[1];
        }
        return (byte) 0xff;
    }

    public String errorString() {
        String errorString = new String();
        if (mType == FAILED) {
            byte errorCode = mData[1];
            switch (errorCode) {
                case 0:
                    errorString = "Prohibited";
                    break;
                case 1:
                    errorString = "Invalid PDU";
                    break;
                case 2:
                    errorString = "Invalid format";
                    break;
                case 3:
                    errorString = "Unexpected PDU";
                    break;
                case 4:
                    errorString = "Confirmation failed";
                    break;
                case 5:
                    errorString = "Out of resources";
                    break;
                case 6:
                    errorString = "Decryption failed";
                    break;
                case 7:
                    errorString = "Unexpected error";
                    break;
                case 8:
                    errorString = "Cannot assign address";
                    break;
                default:
                    errorString = "Error code not supported";
                    break;
            }
        }
        return errorString;
    }

    public void setRandom(byte[] randomBytes) {
        System.arraycopy(randomBytes, 0, mData, 1, 16);
    }

    public byte[] getConfirmation() {
        byte[] data = new byte[16];
        System.arraycopy(mData, 1, data, 0, 16);
        return data;
    }

    public byte[] getRandom() {
        byte[] data = new byte[16];
        System.arraycopy(mData, 1, data, 0, 16);
        return data;
    }

    public void setData(byte[] provisionData) {
        System.arraycopy(provisionData, 0, mData, 1, 25 + 8);
    }
}
