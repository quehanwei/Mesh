package ru.ximen.mesh;

import org.json.JSONObject;

/**
 * Created by ximen on 12.05.18.
 */

public class MeshDevice {
    private short address;

    public MeshDevice(JSONObject jsonObject) {

    }

    public short getAddress() {
        return address;
    }

    public JSONObject toJSON() {
        return new JSONObject();
    }
}
