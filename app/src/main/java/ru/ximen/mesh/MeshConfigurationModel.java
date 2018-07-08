package ru.ximen.mesh;

import android.content.Context;
import android.util.Log;

public class MeshConfigurationModel extends MeshModel {
    final static private String TAG = "MeshConfigurationModel";

    public MeshConfigurationModel(MeshApplication context, short destination) {
        super(context, MeshModel.ID_CONFIGURATION_MODEL_CLIENT, destination, null);
        Log.d(TAG, "Creating configuration model for destination " + destination);
        addProcedure(new MeshSecureNetworkBeaconProc(this));
        addProcedure(new MeshGATTProxyProc(this));
    }

}
