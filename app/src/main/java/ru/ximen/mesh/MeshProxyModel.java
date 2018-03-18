package ru.ximen.mesh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created by ximen on 17.03.18.
 */

public class MeshProxyModel {
    private final Context mContext;
    final static private String TAG = "MeshProvision";

    public MeshProxyModel(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter(MeshService.ACTION_PROXY_DATA_AVAILABLE);
        mContext.registerReceiver(mGattUpdateReceiver, filter);

    }

    public void send(MeshPDU pdu){

    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "Got proxy data: ");
        }
    };

    public void close(){

    }
}

