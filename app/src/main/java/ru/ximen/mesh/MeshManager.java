package ru.ximen.mesh;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ximen on 20.05.18.
 */

public final class MeshManager {
    private Context mContext;
    private File mDirectory;
    final static private String TAG = "MeshManager";

    public MeshManager(Context mContext, File directory) {
        this.mContext = mContext;
        mDirectory = directory;
    }

    /**
     * Returns list of networks saved on device
     */
    public List<String> listNetworks() {
        File[] files = mDirectory.listFiles();
        List<String> filesList = new ArrayList<>();
        for (File item : files) {
            filesList.add(item.getName());
        }
        return filesList;
    }

    /**
     * Creates new empty network and saves it to the disk
     *
     * @param name Name of network being created
     * @return MeshNetwork object for newly created network
     */
    public MeshNetwork createNetwork(String name) {
        Log.d(TAG, "Creating new network: " + name);
        MeshNetwork network = new MeshNetwork(mContext, name);
        save(network.toJSON());
        return network;
    }

    /**
     * Loads existing network from disk and returns corresponding MeshNetwork object
     *
     * @param name Network name to load
     * @return MeshNetwork object for network name. NULL if such network doesn't exists.
     */
    public MeshNetwork getNetwork(String name) {
        File[] files = mDirectory.listFiles();
        for (File item : files)
            if (item.getName().equals(name)) {
                return new MeshNetwork(mContext, load(name));
            }
        return null;
    }

    /**
     * Deletes existing network name from disk
     *
     * @param name Name of network to delete
     */
    public void deleteNetwork(String name) {

    }

    /**
     * Updates network on disk
     *
     * @param network MeshNetwork object representing changed network
     */
    public void updateNetwork(MeshNetwork network) {
        save(network.toJSON());
    }

    private void save(JSONObject json) {
        Log.d(TAG, "Saving JSON: " + json.toString());
        String filename = "";
        try {
            filename = json.getJSONObject("network").getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Saving Filename: " + filename);
        try {
            FileOutputStream out = new FileOutputStream(mDirectory + "/" + filename);
            out.write(json.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject load(String name) {
        File file = new File(mDirectory, name);
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
        JSONObject json = null;
        try {
            json = new JSONObject(new String(bytes));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;

    }

}
