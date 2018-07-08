package ru.ximen.mesh;

import android.util.Log;

public class MeshConfigurationClient extends MeshClient {
    final static private String TAG = "MeshConfigurationClient";

    public MeshConfigurationClient(MeshApplication context, short destination) {
        super(context, destination);
        Log.d(TAG, "Creating configuration client for destination " + destination);
        addModel(new MeshConfigurationModel(context, destination));
    }

}
