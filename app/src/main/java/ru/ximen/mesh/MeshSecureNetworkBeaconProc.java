package ru.ximen.mesh;

public class MeshSecureNetworkBeaconProc extends MeshGSSProc {
    private final byte[] get_opcode = {(byte) 0x80, 0x09};
    private final byte[] set_opcode = {(byte) 0x80, 0x10};
    private final byte[] status_opcode = {(byte) 0x80, 0x11};

    final static public byte SECURE_NETWORK_BEACON_NOT_BROADCASTING = 0x00;
    final static public byte SECURE_NETWORK_BEACON_BROADCASTING = 0x01;
    final static private String TAG = "MeshSecureNetworkBeacon";

    public void get() {
        super.get(new byte[0]);
    }

    @Override
    public void set(byte[] data) throws IllegalArgumentException {
        if ((data[0] > 1) || (data.length > 1))
            throw new IllegalArgumentException("Secure Network Beacon states > 1 are prohibited");       // 4.2.10 Beacon values 0x02 - 0xff prohibited
        super.set(data);
    }

    public MeshSecureNetworkBeaconProc(MeshConfigurationModel context) {
        super(context, "SecureNetworkBeacon");
        super.get_opcode = this.get_opcode;
        super.set_opcode = this.set_opcode;
        super.status_opcode = this.status_opcode;
    }

}
