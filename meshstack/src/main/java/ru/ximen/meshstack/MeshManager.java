package ru.ximen.meshstack;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>MeshManager class allows to connect to multiple mesh networks on the same device. It manages storing
 * networks in local storage, loading disired network and saving changes.</p>
 * <p>Networks being stored in JSON files</p>
 *
 * @author Sergey Novgorodov on 20.05.18.
 */
public final class MeshManager {
    private MeshStackService mContext;
    private File mDirectory;
    private MeshNetwork currentNetwork;
    final static private String TAG = MeshManager.class.getSimpleName();

    /**
     * <p>Creates new mesh manager with storage folder <b>directory</b> which used to load and save
     * networks files.</p>
     *
     * @param context  {@link MeshStackService} context
     * @param directory directory for storing networks
     */
    public MeshManager(MeshStackService context, File directory) {
        this.mContext = context;
        mDirectory = directory;
    }

    /**
     * Returns list of networks names saved on device. That name can be used to load or delete saved
     * network.
     *
     * @return the List of networks names found in storage folder
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
     * @return {@link MeshNetwork} object for newly created network
     */
    public MeshNetwork createNetwork(String name) {
        Log.d(TAG, "Creating new network: " + name);
        MeshNetwork network = new MeshNetwork(mContext, this, name);
        save(network.toJSON());
        currentNetwork = network;
        return network;
    }

    /**
     * Loads existing network from disk and returns corresponding MeshNetwork object
     *
     * @param name Network name to load
     * @return MeshNetwork object for network name. NULL if such network doesn't exists.
     */
    private MeshNetwork getNetwork(String name) {
        File[] files = mDirectory.listFiles();
        for (File item : files)
            if (item.getName().equals(name)) {
                return new MeshNetwork(mContext, this, load(name));
            }
        return null;
    }

    /**
     * Deletes existing network name from disk
     *
     * @param name Name of the network to delete
     */
    public void deleteNetwork(String name) {
        File file = new File(mDirectory, name);
        file.delete();
    }

    /**
     * Updates currently selected network on disk
     */
    public void updateNetwork() {
        JSONObject json = currentNetwork.toJSON();
        JSONArray app = mContext.getAppManager().toJSON();
        try {
            json.put("applications", app);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        save(json);
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
        FileInputStream in;
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

    /**
     * Selects and loads current network.
     *
     * @param name The name of saved network to be selected
     * @return the {@link MeshNetwork} object representing selected network
     */
    public MeshNetwork selectNetwork(String name) {
        currentNetwork = getNetwork(name);
        try {
            mContext.getAppManager().fromJSON(load(name).getJSONArray("applications"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return currentNetwork;
    }

    /**
     * Return network object currently being used.
     *
     * @return {@link MeshNetwork} object for current network
     */
    public MeshNetwork getCurrentNetwork() {
        return currentNetwork;
    }

}
