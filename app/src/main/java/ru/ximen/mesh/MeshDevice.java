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

    public MeshDevice(JSONObject jsonObject) {

    }

    public MeshDevice(BluetoothDevice device, short address) {
        mAddress = address;
        mMAC = device.getAddress();
    }

    public short getAddress() {
        return mAddress;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("address", Integer.valueOf(mAddress));
            json.put("mac", mMAC);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
