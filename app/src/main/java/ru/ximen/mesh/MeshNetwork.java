package ru.ximen.mesh;

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
import org.spongycastle.pqc.math.ntru.util.Util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.ximen.mesh.MeshService.EXTRA_ADDR;
import static ru.ximen.mesh.MeshService.EXTRA_DATA;
import static ru.ximen.mesh.MeshService.EXTRA_SEQ;

/**
 * Created by ximen on 12.05.18.
 */

public class MeshNetwork {
    private Context mContext;                       // Application context
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
    private LocalBroadcastManager mBroadcastManger;
    private short mAddress = 0x0001;

    final static private String TAG = "MeshNetwork";

    // TODO: Interface input and output filters

    public MeshNetwork(Context context, MeshManager manager, JSONObject json) {
        provisioned = new ArrayList<>();
        parseJSON(json);
        init(context, manager);
    }

    public MeshNetwork(Context context, MeshManager manager, String name) {
        NetKey = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(NetKey);
        NetKeyIndex = 0;
        provisioned = new ArrayList<>();
        mNetworkName = name;
        init(context, manager);
    }

    private void init(Context context, MeshManager manager) {
        mContext = context;
        mManager = manager;
        //NetworkID = MeshEC.k3(Utils.hexString2Bytes("7dd7364cd842ad18c17c2b820c84c3d6")); // test
        NetworkID = MeshEC.k3(NetKey);
        mProxy = new MeshProxyModel(mContext);
        byte[] t = MeshEC.k2(NetKey, new byte[1]);
        Log.d(TAG, Utils.toHexString(t));
        mNID = (byte) (t[0] & 0x7F);
        System.arraycopy(t, 1, mEncryptionKey, 0, 16);
        System.arraycopy(t, 17, mPrivacyKey, 0, 16);
        //Log.d(TAG, "Encryption key: " + Utils.toHexString(mEncryptionKey));
        //Log.d(TAG, "Privacy key: " + Utils.toHexString(mPrivacyKey));

        IntentFilter filter = new IntentFilter(MeshService.ACTION_NETWORK_DATA_AVAILABLE);
        filter.addAction(MeshService.ACTION_MESH_BEACON_DATA_AVAILABLE);
        mBroadcastManger = LocalBroadcastManager.getInstance(mContext);
        mBroadcastManger.registerReceiver(mGattUpdateReceiver, filter);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(MeshService.ACTION_NETWORK_DATA_AVAILABLE)) {
                byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
                Log.d(TAG, "Got network data: " + Utils.toHexString(data));
                MeshNetworkPDU pdu = new MeshNetworkPDU(MeshNetwork.this, data);
                Log.d(TAG, "SEQ: " + pdu.getSEQ());
                if (pdu.isValid()) {
                    final Intent newIntent = new Intent(MeshService.ACTION_TRANSPORT_DATA_AVAILABLE);
                    newIntent.putExtra(EXTRA_DATA, pdu.getTransportPDU());
                    newIntent.putExtra(EXTRA_ADDR, pdu.getSRC());
                    newIntent.putExtra(EXTRA_SEQ, pdu.getSEQ());
                    mBroadcastManger.sendBroadcast(newIntent);
                }
            } else if (action.equals(MeshService.ACTION_MESH_BEACON_DATA_AVAILABLE)) {
                Log.d(TAG, "Got secure network beacon data");
                byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
                if (data[0] == 1) {
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
                } else Log.w(TAG, "Unknown Beacon type");
            }
        }
    };

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

    public byte[] getDeviceKey(short address) {
        for (MeshDevice device : provisioned) {
            if (device.getAddress() == address) return device.getDeviceKey();
        }
        return null;
    }
}
