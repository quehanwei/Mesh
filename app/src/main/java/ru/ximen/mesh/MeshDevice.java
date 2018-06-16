package ru.ximen.mesh;

import android.bluetooth.BluetoothDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Created by ximen on 12.05.18.
 */

public class MeshDevice {
    private short mAddress;
    private String mMAC;
    private String mName;
    private byte[] mDeviceKey;
    private UUID mUUID;
    private BluetoothDevice mBluetoothDevice;

    public MeshDevice(JSONObject json) {
        try {
            mAddress = (short) json.getInt("address");
            mMAC = json.getString("mac");
            mName = json.getString("name");
            mDeviceKey = new BigInteger(json.getString("deviceKey"), 16).toByteArray();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public MeshDevice(BluetoothDevice device, short address, byte[] deviceKey) {
        mAddress = address;
        mMAC = device.getAddress();
        mDeviceKey = deviceKey;
    }

    public short getAddress() {
        return mAddress;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("address", Integer.valueOf(mAddress));
            json.put("mac", mMAC);
            json.put("deviceKey", Utils.toHexString(mDeviceKey));
            json.put("name", mName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

}
