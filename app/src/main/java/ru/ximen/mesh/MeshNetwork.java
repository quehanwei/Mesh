package ru.ximen.mesh;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
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
    private int IVIndex;
    private byte mNID;
    private byte[] mEncryptionKey = new byte[16];
    private byte[] mPrivacyKey = new byte[16];
    private List<MeshDevice> provisioned;
    private MeshManager mManager;
    private MeshProvisionModel mProvisioner;
    private MeshProxyModel mProxy;
    private int SEQ;

    final static private String TAG = "MeshNetwork";

    public MeshNetwork(Context context, MeshManager manager, JSONObject json) {
        mContext = context;
        mManager = manager;
        provisioned = new ArrayList<>();
        parseJSON(json);

        byte[] t = MeshEC.k2(NetKey, new byte[1]);
        mNID = (byte) (t[0] & 0x7F);
        System.arraycopy(t, 1, mEncryptionKey, 0, 16);
        System.arraycopy(t, 17, mPrivacyKey, 0, 16);
        Log.d(TAG, "Encryption key: " + Utils.toHexString(mEncryptionKey));
        Log.d(TAG, "Privacy key: " + Utils.toHexString(mPrivacyKey));
    }

    public MeshNetwork(Context context, MeshManager manager, String name) {
        mContext = context;
        mManager = manager;

        NetKey = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(NetKey);
        NetKeyIndex = 0;
        provisioned = new ArrayList<>();
        mNetworkName = name;

        byte[] t = MeshEC.k2(NetKey, new byte[1]);
        mNID = (byte) (t[0] & 0x7F);
        System.arraycopy(t, 1, mEncryptionKey, 0, 16);
        System.arraycopy(t, 17, mPrivacyKey, 0, 16);
        Log.d(TAG, "Encryption key: " + Utils.toHexString(mEncryptionKey));
        Log.d(TAG, "Privacy key: " + Utils.toHexString(mPrivacyKey));
    }

    public String getName() {
        return mNetworkName;
    }

    private void parseJSON(JSONObject json) {
        try {
            mNetworkName = json.getJSONObject("network").getString("name");
            NetKey = new BigInteger(json.getJSONObject("network").getString("key"), 16).toByteArray();
            NetKeyIndex = (short) json.getJSONObject("network").getInt("keyIndex");
            IVIndex = json.getJSONObject("network").getInt("IVIndex");
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

    public short getNetKeyIndex() {
        return NetKeyIndex;
    }

    public int getIVIndex() {
        return IVIndex;
    }

    //public void addProvisionedDevice(MeshDevice device) {
    //    provisioned.add(device);
    //    mManager.updateNetwork(this);
    //}

    public short getNextUnicastAddress() {
        short last = 2;
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
            network.put("key", new BigInteger(1, NetKey).toString(16));
            network.put("keyIndex", NetKeyIndex);
            network.put("IVIndex", IVIndex);
            for (MeshDevice item : provisioned) devices.put(item.toJSON());
            json.put("network", network);
            json.put("devices", devices);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public List<MeshDevice> getDevices() {
        return provisioned;
    }

    public void deleteDevice(MeshDevice device) {
        provisioned.remove(device);
        mManager.updateNetwork();
    }

    // Provisions unprovisioned device
    public void provisionDevice(BluetoothDevice device,
                                final String name,
                                MeshProvisionModel.MeshProvisionGetOOBCallback getOOBCallback,
                                final MeshProvisionModel.MeshProvisionFinishedOOBCallback finished) {
        mProxy = new MeshProxyModel(mContext);
        mProvisioner = new MeshProvisionModel(mContext, mProxy, this);
        mProvisioner.startProvision(device, getOOBCallback, new MeshProvisionModel.MeshProvisionFinishedOOBCallback() {
            @Override
            public void finished(MeshDevice device, MeshNetwork network) {
                device.setName(name);
                provisioned.add(device);
                mManager.updateNetwork();
                mProvisioner.close();
                mProxy.close();
            }
        });
    }

    public void sendPDU(MeshNetworkPDU pdu) {
        mProxy.send(pdu);
    }

    private void updateIV() {

    }

    public int getNextSeq() {
        SEQ++;
        if (SEQ == 0xFFFFFF) updateIV();
        return SEQ;
    }

    public byte getNID() {
        return mNID;
    }

    public byte[] getPrivacyKey() {
        return mPrivacyKey;
    }

    public byte[] getEncryptionKey() {
        return mEncryptionKey;
    }
}
