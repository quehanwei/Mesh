package ru.ximen.mesh;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ximen on 12.05.18.
 */

public class MeshNetwork {
    private Context mContext;                       // Application context
    private String mNetworkName;
    private byte[] NetKey;
    private short NetKeyIndex;
    private List<MeshDevice> provisioned;

    public MeshNetwork(Context context, JSONObject json) {
        mContext = context;
        provisioned = new ArrayList<>();

        parseJSON(json);
    }

    public MeshNetwork(Context context, String name) {
        NetKey = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(NetKey);
        NetKeyIndex = 0;
        provisioned = new ArrayList<>();
    }

    private void parseJSON(JSONObject json) {
        try {
            mNetworkName = json.getJSONObject("network").getString("name");
            NetKey = json.getJSONObject("network").getString("key").getBytes();
            NetKeyIndex = (short) json.getJSONObject("network").getInt("keyIndex");
            JSONArray devices = json.getJSONArray("devices");
            for (int i = 0; i < devices.length(); i++)
                provisioned.add(new MeshDevice(devices.getJSONObject(i)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public byte[] getNetKey() {
        return NetKey;
    }

    public short addDevice() {
        return getNextUnicastAddress();
    }

    private short getNextUnicastAddress() {
        short last = 0;
        for (MeshDevice item : provisioned) {
            if (item.getAddress() - last > 1) return (short) (last + 1);
            last++;
        }
        return last;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        JSONObject network = new JSONObject();
        JSONArray devices = new JSONArray();
        try {
            network.put("name", mNetworkName);
            network.put("key", NetKey);
            network.put("keyIndex", NetKeyIndex);
            for (MeshDevice item : provisioned) devices.put(item.toJSON());
            json.put("network", network);
            json.put("devices", devices);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}
