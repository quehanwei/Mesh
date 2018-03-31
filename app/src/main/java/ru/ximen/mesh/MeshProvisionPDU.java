package ru.ximen.mesh;

import android.util.Log;

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

    @Override
    public byte[] data() {
        return mData;
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
}
