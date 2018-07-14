package ru.ximen.mesh;

import android.util.Log;

import ru.ximen.meshstack.MeshEC;
import ru.ximen.meshstack.Utils;

public class BluetoothMesh {
    final static private String TAG = "BluetoothMesh";

    public BluetoothMesh() {
        Log.d(TAG, "--== Testing k2 ==--");
        byte[] N = Utils.hexString2Bytes("7dd7364cd842ad18c17c2b820c84c3d6");
        byte[] P = new byte[1];
        MeshEC.k2(N, P);
    }

}
