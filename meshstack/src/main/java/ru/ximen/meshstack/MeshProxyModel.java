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

import static ru.ximen.meshstack.MeshBluetoothService.EXTRA_DATA;

/**
 * Created by ximen on 17.03.18.
 */

// TODO: Proxy Configuration messages

public class MeshProxyModel {
    private final MeshStackService mContext;
    final static private String TAG = MeshProxyModel.class.getSimpleName();
    private ArrayList<Byte> mData;
    private boolean transactionRx;

    public MeshProxyModel(MeshStackService context) {
        mContext = context;
        Log.d(TAG, "Binding service");
        mData = new ArrayList<>();
        mContext.getMeshBluetoothService().registerCallback(MeshBluetoothService.MESH_PROXY_DATA_OUT, new MeshBluetoothService.MeshCharacteristicChangedCallback() {
            @Override
            public void onCharacteristicChanged(byte[] data) {
                byte type = (byte) (data[0] & 0x3f);        // 6.3.1
                byte sar = (byte) (data[0] >>> 6);            // 6.3.1
                switch (sar) {
                    case 0:         // complete message
                        if (!transactionRx) {
                            mData.clear();
                            transactionRx = false;
                        } else {
                            return;
                        }
                        break;
                    case 1:
                        if (!transactionRx) {
                            mData.clear();
                            transactionRx = true;
                        } else {
                            return;
                        }
                        break;
                    case -2:
                        if (!transactionRx) {
                            return;
                        }
                        break;
                    case -1:
                        if (transactionRx) {
                            transactionRx = false;
                        } else {
                            return;
                        }
                        break;
                }
                for (int i = 1; i < data.length; i++) mData.add(data[i]);
                if (!transactionRx) {
                    MeshNetwork network = mContext.getNetworkManager().getCurrentNetwork();
                    switch (type) {
                        case 0x00:
                            network.newPDU(new MeshNetworkPDU(network, toArray(mData)));
                            break;
                        case 0x01:
                            network.newPDU(new MeshBeaconPDU(toArray(mData)));
                            break;
                        case 0x02:
                            //broadcastUpdate(MeshBluetoothService.ACTION_PROXY_CONFIGURATION_DATA_AVAILABLE);
                            break;
                        case 0x03:
                            network.newPDU(new MeshProvisionPDU(toArray(mData)));
                            break;
                        default:
                            Log.d(TAG, "PDU of unknown type received");
                    }
                }
            }
        });
    }

    public void send(MeshPDU pdu) {
        byte sar = 0;
        byte[] data = pdu.data();

        if (pdu instanceof MeshProvisionPDU) sar = (byte) (sar | 3);   // Type of PDU
        if (pdu instanceof MeshNetworkPDU) sar = (byte) (sar | 0);   // Type of PDU

        if (data.length > MeshBluetoothService.MTU - 1) {
            Log.d(TAG, "Splitting PDU");
            byte[] partData = new byte[MeshBluetoothService.MTU - 1];
            for (int i = 0; i < data.length; i += MeshBluetoothService.MTU - 1) {
                if (i == 0) {
                    sar &= 0x3f;
                    sar |= 0x40;        // first segment
                    System.arraycopy(data, i, partData, 0, MeshBluetoothService.MTU - 1);
                    sendPart(sar, partData);
                } else if (data.length - i <= MeshBluetoothService.MTU - 1) {
                    sar &= 0x3f;
                    sar |= 0xC0;        // last segment
                    byte[] lastData = new byte[data.length - i];
                    System.arraycopy(data, i, lastData, 0, data.length - i);
                    sendPart(sar, lastData);
                } else {
                    sar &= 0x3f;
                    sar |= 0x80;        // segment
                    System.arraycopy(data, i, partData, 0, MeshBluetoothService.MTU - 1);
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
            mContext.getMeshBluetoothService().writeProvision(params);
        } else {
            mContext.getMeshBluetoothService().writeProxy(params);
        }
    }

    private byte[] toArray(ArrayList<Byte> data) {
        byte[] result = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) result[i] = data.get(i);
        return result;
    }
}

