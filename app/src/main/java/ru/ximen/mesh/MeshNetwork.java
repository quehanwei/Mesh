package ru.ximen.mesh;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public MeshNetwork(Context context, File file) {
        mContext = context;
        provisioned = new ArrayList<>();
        parseJSON(readFromFile(file));
    }

    public MeshNetwork(Context context, String name) {
        File file = new File(mContext.getFilesDir(), mNetworkName);
        NetKey = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(NetKey);
        NetKeyIndex = 0;
        provisioned = new ArrayList<>();
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        saveToFile(file);
    }

    private String readFromFile(File file) {
        int length = (int) file.length();
        byte[] bytes = new byte[length];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            in.read(bytes);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(bytes);
    }

    private void parseJSON(String jsonString) {
        JSONObject json = null;
        try {
            json = new JSONObject(jsonString);
            mNetworkName = json.getJSONObject("network").getString("name");
            NetKey = json.getJSONObject("network").getString("key").getBytes();
            ;
            NetKeyIndex = (short) json.getJSONObject("network").getInt("keyIndex");
            JSONArray devices = json.getJSONArray("devices");
            for (int i = 0; i < devices.length(); i++)
                provisioned.add(new MeshDevice(devices.getJSONObject(i)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveToFile(File file) {
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
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(json.toString().getBytes());
        } catch (IOException e) {
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
}
