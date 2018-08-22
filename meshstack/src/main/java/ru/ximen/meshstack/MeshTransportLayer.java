package ru.ximen.meshstack;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class MeshTransportLayer {
    final static private String TAG = MeshTransportLayer.class.getSimpleName();
    private byte defaultTTL = 20;
    private MeshStackService mContext;
    //private HashSet<Short, MeshTransportPDU> sendQueue;
    private HashMap<Short, ArrayList<MeshTransportPDU>> receiveQueue;

    public MeshTransportLayer(MeshStackService context) {
        mContext = context;
        receiveQueue = new HashMap<>();
    }

    private void processQueued(short addr, byte SegN) {
        //Log.d(TAG, "Processing queue for " + addr + " and SegN " + SegN);
        ArrayList<MeshTransportPDU> set = receiveQueue.get(addr);
        MeshTransportPDU tpdu = set.get(SegN);
        tpdu = new MeshTransportPDU(tpdu.getSEQ() - SegN, tpdu.getAKF(), tpdu.getAID(), addr, (short) (tpdu.getSEQ() - SegN), tpdu.getSegN(), tpdu.getSegN(), tpdu.getSZMIC());
        tpdu.setData(new byte[0]);
        byte[] result = new byte[0];
        int ack = 0;
        for (int i = 0; i < set.size(); i++) {
            if (i > set.get(i).getSegO()) continue;
            byte[] tdata = set.get(i).getAccessData();
            //Log.d(TAG, "tdata: " + Utils.toHexString(tdata));
            byte[] tresult = new byte[result.length + tdata.length];
            System.arraycopy(result, 0, tresult, 0, result.length);
            System.arraycopy(tdata, 0, tresult, result.length, tdata.length);
            result = tresult;
            //Log.d(TAG, "result: " + Utils.toHexString(result));
            ack += 1 << set.get(i).getSegO();
        }
        sendAck((short) tpdu.getSEQ(), addr, ack);
        set.clear();
        tpdu.setData(result);
        Log.d(TAG, "Big PDU: " + Utils.toHexString(tpdu.data()));

        mContext.getUpperTransportLayer().newPDU(new MeshUpperTransportPDU(tpdu), addr, tpdu.getSZMIC());
    }

    private void sendAck(short SeqZero, short addr, int blockAck) {
        MeshTransportAckPDU pdu = new MeshTransportAckPDU(SeqZero, blockAck);
        MeshNetworkPDU npdu = new MeshNetworkPDU(mContext.getNetworkManager().getCurrentNetwork(), mContext.getNetworkManager().getCurrentNetwork().getNextSeq(), addr, (byte) 1, defaultTTL); // CTL = 0, only access messages
        npdu.setData(pdu.data());
        mContext.getNetworkManager().getCurrentNetwork().sendPDU(npdu);

    }

    public void send(MeshUpperTransportPDU pdu) {
        byte[] data = pdu.data();
        if (data.length > 15) {
            int SEQ = pdu.getSEQ();
            short SeqZero = (short) SEQ;
            for (int i = 0; i < data.length; i += 12) {
                if (i > 0) SEQ = mContext.getNetworkManager().getCurrentNetwork().getNextSeq();
                byte[] segmentData = new byte[((data.length - i) > 12) ? 12 : (data.length - 1)];
                System.arraycopy(data, i, segmentData, 0, segmentData.length);
                MeshTransportPDU tpdu = new MeshTransportPDU(SEQ, pdu.getAKF(), pdu.getAID(), pdu.getDST(), SeqZero, (byte) (i / 12), (byte) Math.ceil(data.length / 12), false);
                tpdu.setData(segmentData);
                MeshNetworkPDU npdu = new MeshNetworkPDU(mContext.getNetworkManager().getCurrentNetwork(), SEQ, tpdu.getDST(), (byte) 0, defaultTTL); // CTL = 0, only access messages
                npdu.setData(tpdu.data());
                mContext.getNetworkManager().getCurrentNetwork().sendPDU(npdu);
            }
        } else {
            MeshTransportPDU tpdu = new MeshTransportPDU(pdu.getSEQ(), pdu.getAKF(), pdu.getAID(), pdu.getDST());
            tpdu.setData(data);
            //Log.d(TAG, "Transport PDU: " + Utils.toHexString(tpdu.data()));
            MeshNetworkPDU npdu = new MeshNetworkPDU(mContext.getNetworkManager().getCurrentNetwork(), tpdu.getSEQ(), tpdu.getDST(), (byte) 0, defaultTTL); // CTL = 0, only access messages
            npdu.setData(tpdu.data());
            //Log.d(TAG, "Network PDU: " + Utils.toHexString(npdu.data()));
            mContext.getNetworkManager().getCurrentNetwork().sendPDU(npdu);
        }
    }

    public void newPDU(MeshTransportPDU pdu, short addr){
        if (pdu.isComplete()) {
            Log.d(TAG, "Complete PDU: " + Utils.toHexString(pdu.getAccessData()));
            mContext.getUpperTransportLayer().newPDU(new MeshUpperTransportPDU(pdu), addr, pdu.getSZMIC());
        } else {
            Log.d(TAG, "Uncomplete PDU: " + Utils.toHexString(pdu.data()));
            ArrayList<MeshTransportPDU> set = receiveQueue.get(addr);
            if (null == set) {
                set = new ArrayList<>();
                receiveQueue.put(addr, set);
            }
            set.add(pdu.getSegO(), pdu);
            //Log.d(TAG, "Adding incomplete pdu to index " + pdu.getSegO());
            if (pdu.isLast()) processQueued(addr, pdu.getSegN());
        }

    }
}
