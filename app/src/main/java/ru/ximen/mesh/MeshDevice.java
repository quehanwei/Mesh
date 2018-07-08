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
    private boolean mProxy = true;
    //private BluetoothDevice mBluetoothDevice;

    public MeshDevice(JSONObject json) {
        try {
            mAddress = (short) json.getInt("address");
            mMAC = json.getString("mac");
            mName = json.getString("name");
            mDeviceKey = Utils.hexString2Bytes(json.getString("deviceKey"));
            JSONArray features = json.getJSONArray("features");
            for (int i = 0; i < features.length(); i++) {
                if (features.getString(i).equals("proxy")) mProxy = true;
            }
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
            JSONArray features = new JSONArray();
            if (isProxy()) features.put("proxy");
            json.put("features", features);
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

    public String getMAC() {
        return mMAC;
    }

    public byte[] getDeviceKey() {
        return mDeviceKey;
    }

    public boolean isProxy() {
        return mProxy;    // TODO: Replace to configuration result
    }
}
