package ru.ximen.meshstack;

import android.bluetooth.BluetoothDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
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
    private HashMap<Byte, byte[]> models;
    //private BluetoothDevice mBluetoothDevice;

    public MeshDevice(JSONObject json) {
        models = new HashMap<>();
        try {
            mAddress = (short) json.getInt("address");
            mMAC = json.getString("mac");
            mName = json.getString("name");
            mDeviceKey = Utils.hexString2Bytes(json.getString("deviceKey"));
            JSONArray features = json.getJSONArray("features");
            for (int i = 0; i < features.length(); i++) {
                if (features.getString(i).equals("proxy")) mProxy = true;
            }
            JSONArray mod = json.getJSONArray("models");
            for (int i = 0; i < mod.length(); i++) {
                JSONObject obj = mod.getJSONObject(i);
                byte AID = (byte)obj.getInt("aid");
                String m = obj.getString("model");
                models.put(AID, Utils.hexString2Bytes(m));
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
            JSONArray mod = new JSONArray();
            for (Map.Entry<Byte, byte[]> entry : models.entrySet()) {
                JSONObject obj = new JSONObject();
                obj.put("model", Utils.toHexString(entry.getValue()));
                obj.put("aid", entry.getKey());
                mod.put(obj);
            }
            json.put("models", mod);
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
