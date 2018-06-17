package ru.ximen.mesh;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MeshNetworkPDU extends MeshPDU {
    private byte IVI;
    private byte NID;
    private byte CTL;
    private byte TTL;
    private int SEQ;
    private short SRC;
    private short DST;
    private byte[] transportPDU;
    private byte[] NetMIC;

    public MeshNetworkPDU(MeshNetwork network, short SRC, short DST, byte CTL, byte TTL) {
        this.CTL = CTL;
        this.TTL = TTL;
        if (CTL == 0) NetMIC = new byte[4];
        else NetMIC = new byte[8];
        this.SEQ = network.getNextSeq();
        this.SRC = SRC;
        this.DST = DST;
        IVI = (byte) (network.getIVIndex() & 0x1);
        // NID described int 3.8.6.3.1
    }

    void setTransportPDU(byte[] pdu) {
        transportPDU = new byte[pdu.length];
        System.arraycopy(pdu, 0, transportPDU, 0, pdu.length);
        setNetMIC();
    }

    private void setNetMIC() {

    }


    @Override
    public byte[] data() {
        byte[] header = new byte[9];
        header[0] = (byte) ((IVI << 7) + NID);
        header[1] = (byte) ((CTL << 7) + TTL);
        header[4] = (byte) (SEQ & 0xFF);
        header[3] = (byte) ((SEQ >> 8) & 0xFF);
        header[2] = (byte) (SEQ >> 16);
        header[6] = (byte) (SRC & 0xFF);
        header[5] = (byte) (SRC >> 8);
        header[8] = (byte) (DST & 0xFF);
        header[7] = (byte) (DST >> 8);
        byte[] data = new byte[header.length + transportPDU.length + NetMIC.length];
        System.arraycopy(header, 0, data, 0, header.length);
        System.arraycopy(transportPDU, 0, data, header.length, transportPDU.length);
        System.arraycopy(NetMIC, 0, data, header.length + transportPDU.length, NetMIC.length);
        return data;
    }
}
