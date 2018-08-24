package ru.ximen.meshstack;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
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
    private MeshStackService mContext;                       // Application context
    private String mNetworkName;
    private byte[] NetKey;
    private short NetKeyIndex;
    private byte[] NetworkID;
    private int IVIndex;
    private byte mNID;
    private byte[] mEncryptionKey = new byte[16];
    private byte[] mPrivacyKey = new byte[16];
    private List<MeshDevice> provisioned;
    private MeshManager mManager;
    private MeshProvisionModel mProvisioner;
    private MeshProxyModel mProxy;
    private int SEQ;
    private short mAddress = 0x0001;

    final static private String TAG = MeshNetwork.class.getSimpleName();

    // TODO: Interface input and output filters

    public MeshNetwork(MeshStackService context, MeshManager manager, JSONObject json) {
        provisioned = new ArrayList<>();
        parseJSON(json);
        init(context, manager);
    }

    public MeshNetwork(MeshStackService context, MeshManager manager, String name) {
        NetKey = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(NetKey);
        NetKeyIndex = 0;
        provisioned = new ArrayList<>();
        mNetworkName = name;
        init(context, manager);
    }

    private void init(MeshStackService context, MeshManager manager) {
        mContext = context;
        mManager = manager;
        //NetworkID = MeshEC.k3(Utils.hexString2Bytes("7dd7364cd842ad18c17c2b820c84c3d6")); // test
        NetworkID = MeshEC.k3(NetKey);
        mProxy = mContext.getProxy();
        byte[] t = MeshEC.k2(NetKey, new byte[1]);
        Log.d(TAG, Utils.toHexString(t));
        mNID = (byte) (t[0] & 0x7F);
        System.arraycopy(t, 1, mEncryptionKey, 0, 16);
        System.arraycopy(t, 17, mPrivacyKey, 0, 16);
    }

    public void newPDU(MeshNetworkPDU pdu){
        Log.d(TAG, "SEQ: " + pdu.getSEQ());
        if (pdu.isValid()) {
            mContext.getTransportLayer().newPDU(new MeshTransportPDU(pdu.getTransportPDU(), pdu.getSEQ()), pdu.getSRC());
        }
    }

    public void newPDU(MeshBeaconPDU pdu){
        Log.d(TAG, "Got secure network beacon data");
        /*if (data[0] == 1) {
            if ((data[1] & 0x01) == 1) Log.d(TAG, "Key refresh being in progress");
            if ((data[1] & 0x02) == 2) Log.d(TAG, "IV update active");
            byte[] networkID = new byte[8];
            byte[] ivi = new byte[4];
            System.arraycopy(data, 2, networkID, 0, 8);
            Log.d(TAG, "Network ID: " + Utils.toHexString(networkID));
            if (!Arrays.equals(networkID, NetworkID))
                Log.e(TAG, "Network ID mismatch! Must be " + Utils.toHexString(NetworkID));
            System.arraycopy(data, 10, ivi, 0, 4);
            Log.d(TAG, "IV Index: " + Utils.toHexString(ivi));
            // Todo: check auth value
        } else Log.w(TAG, "Unknown Beacon type");*/
    }

    public void newPDU(MeshProvisionPDU pdu){

    }

    public short getAddress() {
        return mAddress;
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
            mAddress = (short) json.getJSONObject("network").getInt("address");
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
            network.put("address", mAddress);
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
        mProvisioner = new MeshProvisionModel(mContext);
        mProvisioner.startProvision(device, getOOBCallback, new MeshProvisionModel.MeshProvisionFinishedOOBCallback() {
            @Override
            public void finished(MeshDevice device, MeshNetwork network) {
                device.setName(name);
                provisioned.add(device);
                mManager.updateNetwork();
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

    public byte[] getDeviceKey(short address) {
        for (MeshDevice device : provisioned) {
            if (device.getAddress() == address) return device.getDeviceKey();
        }
        return null;
    }
}
