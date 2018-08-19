package ru.ximen.meshstack;

import java.util.ArrayList;

public class MeshCompositionData {
    public class Element{
        private short descriptor;
        private ArrayList<byte[]> sigModels;
        private ArrayList<byte[]> vendorModels;

        public Element(short descriptor, ArrayList<byte[]> sigModels, ArrayList<byte[]> vendorModels) {
            this.descriptor = descriptor;
            this.sigModels = sigModels;
            this.vendorModels = vendorModels;
        }

        public ArrayList<byte[]> getSigModels() {
            return sigModels;
        }

        public ArrayList<byte[]> getVendorModels() {
            return vendorModels;
        }
    }

    private short CID;
    private short PID;
    private short VID;
    private short CRPL;
    private boolean relay;
    private boolean proxy;
    private boolean friend;
    private boolean lowPower;
    private ArrayList<Element> elements;

    public MeshCompositionData(byte[] data) {
        CID = (short)((data[0] << 8) + data[1]);
        PID = (short)((data[2] << 8) + data[3]);
        VID = (short)((data[4] << 8) + data[5]);
        CRPL = (short)((data[6] << 8) + data[7]);
        relay = (data[9] & 0x01) == 1;
        proxy = (data[9] & 0x02) == 2;
        friend = (data[9] & 0x04) == 4;
        lowPower = (data[9] & 0x08) == 8;
        int i = 10;
            short descriptor = (short)((data[i] << 8) + data[i+1]);
            byte nums  = data[i+2];
            byte numv  = data[i+3];
            ArrayList<byte[]> sigModels = new ArrayList<>();
            ArrayList<byte[]> vendorModels = new ArrayList<>();
            i+=4;
            for (int j = 0; j < nums; j++) {
                byte[] t = new byte[2];
                t[0] = data[i];
                t[1] = data[i+1];
                sigModels.add(t);
                i+=(j+1)*2;
            }
            for (int j = 0; j < nums; j++) {
                byte[] t = new byte[4];
                t[0] = data[i];
                t[1] = data[i+1];
                t[2] = data[i+2];
                t[3] = data[i+3];
                i+=(j+1)*4;
            }
            elements = new ArrayList<>();
        elements.add(new Element(descriptor, sigModels, vendorModels));
    }

    public short getCID() {
        return CID;
    }

    public short getPID() {
        return PID;
    }

    public short getVID() {
        return VID;
    }

    public short getCRPL() {
        return CRPL;
    }

    public boolean isRelay() {
        return relay;
    }

    public boolean isProxy() {
        return proxy;
    }

    public boolean isFriend() {
        return friend;
    }

    public boolean isLowPower() {
        return lowPower;
    }

    public ArrayList<Element> getElements() {
        return elements;
    }
}
