package ru.ximen.meshstack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static ru.ximen.meshstack.MeshService.EXTRA_DATA;

/**
 * Created by ximen on 17.03.18.
 */

// TODO: Proxy Configuration messages

public class MeshProxyModel {
    private final Context mContext;
    final static private String TAG = "MeshProxy";
    private LocalBroadcastManager mBroadcastManager;
    private List<Byte> mData;
    private boolean transactionRx;
    private boolean transactionTx;
    private boolean mBound;

    public MeshProxyModel(Context context) {
        mContext = context;
        Log.d(TAG, "Binding service");
        IntentFilter filter = new IntentFilter(MeshService.ACTION_PROXY_DATA_AVAILABLE);
        mBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        mBroadcastManager.registerReceiver(mGattUpdateReceiver, filter);
        mData = new ArrayList<>();
    }

    public void send(MeshPDU pdu) {
        byte sar = 0;
        byte[] data = pdu.data();

        if (pdu instanceof MeshProvisionPDU) sar = (byte) (sar | 3);   // Type of PDU
        if (pdu instanceof MeshNetworkPDU) sar = (byte) (sar | 0);   // Type of PDU

        if (data.length > MeshService.MTU - 1) {
            Log.d(TAG, "Splitting PDU");
            byte[] partData = new byte[MeshService.MTU - 1];
            for (int i = 0; i < data.length; i += MeshService.MTU - 1) {
                if (i == 0) {
                    sar &= 0x3f;
                    sar |= 0x40;        // first segment
                    System.arraycopy(data, i, partData, 0, MeshService.MTU - 1);
                    sendPart(sar, partData);
                } else if (data.length - i <= MeshService.MTU - 1) {
                    sar &= 0x3f;
                    sar |= 0xC0;        // last segment
                    byte[] lastData = new byte[data.length - i];
                    System.arraycopy(data, i, lastData, 0, data.length - i);
                    sendPart(sar, lastData);
                } else {
                    sar &= 0x3f;
                    sar |= 0x80;        // segment
                    System.arraycopy(data, i, partData, 0, MeshService.MTU - 1);
                    sendPart(sar, partData);
                }
            }
        } else {
            sendPart(sar, data);
        }
    }

    private void sendPart(byte sar, byte[] data) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] params = new byte[data.length + 1];
        params[0] = sar;
        System.arraycopy(data, 0, params, 1, data.length);
        Log.d(TAG, "Sending: " + new BigInteger(1, params).toString(16));
        if ((sar & 0x3f) == 3) {
            ((MeshApplication) mContext.getApplicationContext()).getMeshService().writeProvision(params);
        } else {
            ((MeshApplication) mContext.getApplicationContext()).getMeshService().writeProxy(params);
        }
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
            byte type = (byte) (data[0] & 0x3f);        // 6.3.1
            byte sar = (byte) (data[0] >>> 6);            // 6.3.1
            switch (sar) {
                case 0:         // complete message
                    if (!transactionRx) {
                        Log.d(TAG, "Reconstructing complete PDU from data " + Utils.toHexString(data));
                        mData.clear();
                        transactionRx = false;
                    } else {
                        Log.d(TAG, "Complete PDU while waiting segment: " + Utils.toHexString(data));
                        return;
                    }
                    break;
                case 1:
                    if (!transactionRx) {
                        Log.d(TAG, "Reconstructing first PDU segment from data " + Utils.toHexString(data));
                        mData.clear();
                        transactionRx = true;
                    } else {
                        Log.d(TAG, "First PDU while waiting segment: " + Utils.toHexString(data));
                        return;
                    }
                    break;
                case -2:
                    if (transactionRx) {
                        Log.d(TAG, "Reconstructing PDU segment from data " + Utils.toHexString(data));
                        transactionRx = true;
                    } else {
                        Log.d(TAG, "Segment PDU without first: " + Utils.toHexString(data));
                        return;
                    }
                    break;
                case -1:
                    if (transactionRx) {
                        Log.d(TAG, "Reconstructing last PDU segment from data " + Utils.toHexString(data));
                        transactionRx = false;
                    } else {
                        Log.d(TAG, "Last PDU without first: " + Utils.toHexString(data));
                        return;
                    }
                    break;
            }
            for (int i = 1; i < data.length; i++) mData.add(data[i]);
            if (!transactionRx) {
                switch (type) {
                    case 0x00:
                        broadcastUpdate(MeshService.ACTION_NETWORK_DATA_AVAILABLE);
                        break;
                    case 0x01:
                        broadcastUpdate(MeshService.ACTION_MESH_BEACON_DATA_AVAILABLE);
                        break;
                    case 0x02:
                        broadcastUpdate(MeshService.ACTION_PROXY_CONFIGURATION_DATA_AVAILABLE);
                        break;
                    case 0x03:
                        broadcastUpdate(MeshService.ACTION_PROVISION_DATA_AVAILABLE);
                        break;
                    default:
                        Log.d(TAG, "PDU of unknown type received");
                }
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        if (mData != null && mData.size() > 0) {
            byte[] result = new byte[mData.size()];
            for (int i = 0; i < mData.size(); i++) result[i] = mData.get(i);
            intent.putExtra(EXTRA_DATA, result);
        }
        mBroadcastManager.sendBroadcast(intent);
    }

    public void close() {
        mBroadcastManager.unregisterReceiver(mGattUpdateReceiver);
    }

}

