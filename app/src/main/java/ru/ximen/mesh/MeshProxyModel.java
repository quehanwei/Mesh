package ru.ximen.mesh;

import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.ximen.mesh.MeshService.EXTRA_DATA;

/**
 * Created by ximen on 17.03.18.
 */

public class MeshProxyModel {
    private final Context mContext;
    final static private String TAG = "MeshProxy";
    private BluetoothGatt mBluetoothGatt;
    private LocalBroadcastManager mBroadcastManager;
    private List<Byte> mData;
    private boolean transactionRx;
    private boolean transactionTx;

    public MeshProxyModel(Context context, BluetoothGatt gatt) {
        mContext = context;
        IntentFilter filter = new IntentFilter(MeshService.ACTION_PROXY_DATA_AVAILABLE);
        mBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        mBroadcastManager.registerReceiver(mGattUpdateReceiver, filter);
        mBluetoothGatt = gatt;
        mData = new ArrayList<>();
    }

    public void send(MeshPDU pdu){
        byte sar = 0;
        byte[] data = pdu.data();

        if (pdu instanceof MeshProvisionPDU) sar = (byte) (sar | 3);   // Type of PDU

        if (data.length > MeshService.MTU) {
            Log.d(TAG, "Splitting PDU");
            byte[] partData = new byte[MeshService.MTU];
            for (int i = 0; i < data.length; i += MeshService.MTU) {
                if (i == 0) {
                    sar &= 0x3f;
                    sar |= 0x40;        // first segment
                    System.arraycopy(data, i, partData, 0, MeshService.MTU);
                } else if (data.length - i <= MeshService.MTU) {
                    sar &= 0x3f;
                    sar |= 0xC0;        // last segment
                    byte[] lastData = new byte[data.length - i];
                    System.arraycopy(data, i, lastData, 0, data.length - i);
                } else {
                    sar &= 0x3f;
                    sar |= 0x80;        // segment
                    System.arraycopy(data, i, partData, 0, MeshService.MTU);
                }
                sendPart(sar, partData);
            }
        } else {
            sendPart(sar, data);
        }
    }

    private void sendPart(byte sar, byte[] data) {
        MeshService service = BluetoothMesh.getInstance().getService();
        byte[] params = new byte[data.length + 1];
        params[0] = sar;
        System.arraycopy(data, 0, params, 1, data.length);
        Log.d(TAG, "Sending: " + Arrays.toString(params));
        service.writeProvision(params);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
            Log.d(TAG, "Reconstructing PDU from data " + Arrays.toString(data));
            byte type = (byte) (data[0] & 0x3f);        // 6.3.1
            byte sar = (byte) (data[0] >>> 6);            // 6.3.1
            switch (sar) {
                case 0:         // complete message
                    mData.clear();
                    transactionRx = false;
                    break;
                case 1:
                    mData.clear();
                    transactionRx = true;
                    break;
                case 2:
                    transactionRx = true;
                    break;
                case 3:
                    transactionRx = false;
                    break;
            }
            for (int i = 1; i < data.length; i++) mData.add(data[i]);
            if (!transactionRx) {
                if (type == 0x03) {      // Provision PDU, ignoring SAR
                    broadcastUpdate(MeshService.ACTION_PROVISION_DATA_AVAILABLE);
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

    public void close(){
        mBroadcastManager.unregisterReceiver(mGattUpdateReceiver);
    }
}

