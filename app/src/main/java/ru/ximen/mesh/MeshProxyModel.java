package ru.ximen.mesh;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
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
    private boolean transaction;

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
        MeshService service = BluetoothMesh.getInstance().getService();

        if (data.length > MeshService.MTU) {
            Log.d(TAG, "Splitting PDU");
        } else {
            if (pdu instanceof MeshProvisionPDU) sar = 3;   // Type of PDU
        }
        byte[] params = new byte[data.length + 1];
        params[0] = sar;
        System.arraycopy(data, 0, params, 1, data.length);

        service.writeProvision(params);
        Log.d(TAG, "Sending: " + Arrays.toString(params));
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
                    transaction = false;
                    break;
                case 1:
                    mData.clear();
                    transaction = true;
                    break;
                case 2:
                    transaction = true;
                    break;
                case 3:
                    transaction = false;
                    break;
            }
            for (int i = 1; i < data.length; i++) mData.add(data[i]);
            if (!transaction) {
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

