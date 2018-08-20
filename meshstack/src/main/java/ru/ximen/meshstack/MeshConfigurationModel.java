package ru.ximen.meshstack;

import android.util.Log;

public class MeshConfigurationModel extends MeshModel {
    final static private String TAG = MeshConfigurationModel.class.getSimpleName();

    public MeshConfigurationModel(MeshStackService context, short destination) {
        super(context, MeshModel.ID_CONFIGURATION_MODEL_CLIENT, destination, null);
        Log.d(TAG, "Creating configuration model for destination " + destination);
        addProcedure(new MeshSecureNetworkBeaconProc(this));
        addProcedure(new MeshGATTProxyProc(this));
        addProcedure(new MeshCompositionDataProc(this));
        addProcedure(new MeshRelayProc(this));
        addProcedure(new MeshDefaultTTLProc(this));
        addProcedure(new MeshAppKeyProc(this));
    }

}
