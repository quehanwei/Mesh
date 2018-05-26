package ru.ximen.mesh;

import android.bluetooth.BluetoothDevice;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by ximen on 12.05.18.
 */

public class MeshDevice {
    private short mAddress;

    public MeshDevice(JSONObject jsonObject) {

    }

    public MeshDevice(BluetoothDevice device, short address) {

    }

    public short getAddress() {
        return mAddress;
    }

    public JSONObject toJSON() {
        return new JSONObject();
    }
}
