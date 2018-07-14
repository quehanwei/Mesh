package ru.ximen.meshstack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.spongycastle.pqc.math.ntru.util.Util;

import java.util.ArrayList;
import java.util.HashMap;

import static ru.ximen.meshstack.MeshService.EXTRA_ADDR;
import static ru.ximen.meshstack.MeshService.EXTRA_DATA;
import static ru.ximen.meshstack.MeshService.EXTRA_SEQ;

public class MeshTransportLayer {
    final static private String TAG = "MeshTransportLayer";
    private byte defaultTTL = 20;
    private MeshApplication mContext;
    private LocalBroadcastManager mBroadcastManger;
    //private HashSet<Short, MeshTransportPDU> sendQueue;
    private HashMap<Short, ArrayList<MeshTransportPDU>> receiveQueue;

    public MeshTransportLayer(MeshApplication context) {
        mContext = context;
        receiveQueue = new HashMap<>();
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
                ArrayList<MeshTransportPDU> set = receiveQueue.get(addr);
                if (null == set) {
                    Log.d(TAG, "Creating new queue for " + addr);
                    set = new ArrayList<>();
                    receiveQueue.put(addr, set);
                }
                set.add(pdu.getSegO(), pdu);
                Log.d(TAG, "Adding incomplete pdu to index " + pdu.getSegO());
                if (pdu.isLast()) processQueued(addr, pdu.getSegN());
                // ToDo: Ack
            }
        }
    };

    private void processQueued(short addr, byte SegN) {
        // Todo: Assemble Upper Transport PDU and broadcast access data intent
        Log.d(TAG, "Processing queue for " + addr + " and SegN " + SegN);
        ArrayList<MeshTransportPDU> set = receiveQueue.get(addr);
        MeshTransportPDU tpdu = set.get(SegN);
        Log.d(TAG, "Last PDU: " + Utils.toHexString(tpdu.data()));
        tpdu = new MeshTransportPDU(tpdu.getSEQ() - SegN, tpdu.getAKF(), tpdu.getAID(), addr, (short) (tpdu.getSEQ() - SegN), tpdu.getSegN(), tpdu.getSegN(), tpdu.getSZMIC());
        tpdu.setData(new byte[0]);
        Log.d(TAG, "New PDU: " + Utils.toHexString(tpdu.data()));
        byte[] result = new byte[0];
        for (int i = 0; i < set.size(); i++) {
            if (i > set.get(i).getSegO()) continue;
            byte[] tdata = set.get(i).getAccessData();
            Log.d(TAG, "tdata: " + Utils.toHexString(tdata));
            byte[] tresult = new byte[result.length + tdata.length];
            System.arraycopy(result, 0, tresult, 0, result.length);
            System.arraycopy(tdata, 0, tresult, result.length, tdata.length);
            result = tresult;
            Log.d(TAG, "result: " + Utils.toHexString(result));
        }
        set.clear();
        tpdu.setData(result);
        Log.d(TAG, "Big PDU: " + Utils.toHexString(tpdu.data()));

        final Intent newIntent = new Intent(MeshService.ACTION_UPPER_TRANSPORT_DATA_AVAILABLE);
        newIntent.putExtra(EXTRA_DATA, tpdu.data());
        newIntent.putExtra(EXTRA_ADDR, addr);
        newIntent.putExtra(EXTRA_SEQ, tpdu.getSEQ());
        mBroadcastManger.sendBroadcast(newIntent);
    }

    public void send(MeshUpperTransportPDU pdu) {
        byte[] data = pdu.data();
        if (data.length > 15) {
            int SEQ = pdu.getSEQ();
            short SeqZero = (short) SEQ;
            for (int i = 0; i < data.length; i += 12) {
                if (i > 0) SEQ = mContext.getManager().getCurrentNetwork().getNextSeq();
                byte[] segmentData = new byte[((data.length - i) > 12) ? 12 : (data.length - 1)];
                System.arraycopy(data, i, segmentData, 0, segmentData.length);
                MeshTransportPDU tpdu = new MeshTransportPDU(SEQ, pdu.getAKF(), pdu.getAID(), pdu.getDST(), SeqZero, (byte) (i / 12), (byte) Math.ceil(data.length / 12), false);
                tpdu.setData(segmentData);
                MeshNetworkPDU npdu = new MeshNetworkPDU(mContext.getManager().getCurrentNetwork(), SEQ, tpdu.getDST(), (byte) 0, defaultTTL); // CTL = 0, only access messages
                npdu.setData(tpdu.data());
                mContext.getManager().getCurrentNetwork().sendPDU(npdu);
            }
        } else {
            MeshTransportPDU tpdu = new MeshTransportPDU(pdu.getSEQ(), pdu.getAKF(), pdu.getAID(), pdu.getDST());
            tpdu.setData(data);
            //Log.d(TAG, "Transport PDU: " + Utils.toHexString(tpdu.data()));
            MeshNetworkPDU npdu = new MeshNetworkPDU(mContext.getManager().getCurrentNetwork(), tpdu.getSEQ(), tpdu.getDST(), (byte) 0, defaultTTL); // CTL = 0, only access messages
            npdu.setData(tpdu.data());
            //Log.d(TAG, "Network PDU: " + Utils.toHexString(npdu.data()));
            mContext.getManager().getCurrentNetwork().sendPDU(npdu);
        }
    }
}
