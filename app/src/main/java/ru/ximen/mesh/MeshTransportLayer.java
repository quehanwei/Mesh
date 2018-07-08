package ru.ximen.mesh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.LinkedHashMap;

import static ru.ximen.mesh.MeshService.EXTRA_ADDR;
import static ru.ximen.mesh.MeshService.EXTRA_DATA;
import static ru.ximen.mesh.MeshService.EXTRA_SEQ;

public class MeshTransportLayer {
    final static private String TAG = "MeshTransportLayer";
    private byte defaultTTL = 20;
    private MeshApplication mContext;
    private LocalBroadcastManager mBroadcastManger;
    private LinkedHashMap<Short, MeshTransportPDU> sendQueue;
    private LinkedHashMap<Short, MeshTransportPDU> receiveQueue;

    public MeshTransportLayer(MeshApplication context) {
        mContext = context;

        IntentFilter filter = new IntentFilter(MeshService.ACTION_TRANSPORT_DATA_AVAILABLE);
        mBroadcastManger = LocalBroadcastManager.getInstance(mContext);
        mBroadcastManger.registerReceiver(mGattUpdateReceiver, filter);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
            Log.d(TAG, "Got transport data: " + Utils.toHexString(data));
            short addr = intent.getShortExtra(EXTRA_ADDR, (short) 0);
            int SEQ = intent.getIntExtra(EXTRA_SEQ, 0);
            MeshTransportPDU pdu = new MeshTransportPDU(data, SEQ);
            if (pdu.isComplete()) {
                Log.d(TAG, "Complete PDU: " + Utils.toHexString(pdu.getAccessData()));
                final Intent newIntent = new Intent(MeshService.ACTION_UPPER_TRANSPORT_DATA_AVAILABLE);
                newIntent.putExtra(EXTRA_DATA, pdu.data());
                newIntent.putExtra(EXTRA_ADDR, addr);
                newIntent.putExtra(EXTRA_SEQ, SEQ);
                mBroadcastManger.sendBroadcast(newIntent);
            } else {
                Log.d(TAG, "Uncomplete PDU: " + Utils.toHexString(pdu.data()));
                if (pdu.isLast()) processQueued();
                else receiveQueue.put(addr, pdu);
            }
        }
    };

    private void processQueued() {
        // Todo: Assemble Upper Transport PDU and broadcast access data intenet
    }

    public void send(MeshUpperTransportPDU pdu) {
        if (pdu.data().length > 15) {
            // Segment pdu
        } else {
            MeshTransportPDU tpdu = new MeshTransportPDU(pdu.getSEQ(), pdu.getAKF(), pdu.getAID(), pdu.getDST());
            tpdu.setData(pdu.data());
            Log.d(TAG, "Transport PDU: " + Utils.toHexString(tpdu.data()));
            MeshNetworkPDU npdu = new MeshNetworkPDU(mContext.getManager().getCurrentNetwork(), tpdu.getSEQ(), tpdu.getDST(), (byte) 0, defaultTTL); // CTL = 0, only access messages
            npdu.setData(tpdu.data());
            Log.d(TAG, "Network PDU: " + Utils.toHexString(npdu.data()));
            mContext.getManager().getCurrentNetwork().sendPDU(npdu);
        }
    }
}
