package ru.ximen.mesh;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ximen on 12.05.18.
 */

public class MeshNetwork {
    private byte[] NetKey;
    private short NetKeyIndex;
    private List<MeshDevice> provisioned;

    public MeshNetwork() {
        NetKey = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(NetKey);
        NetKeyIndex = 0;

        provisioned = new ArrayList<>();
    }

    public byte[] getNetKey() {
        return NetKey;
    }

    public short addDevice() {
        return getNextUnicastAddress();
    }

    private short getNextUnicastAddress() {
        short last = 0;
        for (MeshDevice item : provisioned) {
            if (item.getAddress() - last > 1) return (short) (last + 1);
            last++;
        }
        return last;
    }
}
