package ru.ximen.mesh;

import java.util.ArrayList;
import java.util.List;

public abstract class MeshModel {
    final static public short ID_CONFIGURATION_MODEL_SERVER = 0x0000;
    final static public short ID_CONFIGURATION_MODEL_CLIENT = 0x0001;
    final static public short ID_HEALTH_MODEL_SERVER = 0x0002;
    final static public short ID_HEALTH_MODEL_CLIENT = 0x0003;
    final static public short ID_ONOFF_MODEL_CLIENT = 0x1001;
    final static public short ID_ONOFF_MODEL_SERVER = 0x1000;

    private MeshApplication mContext;
    protected short modelID;
    protected short vendorID;
    private List<MeshProcedure> procs;
    private short mDestination;
    private byte[] mAppKey;
    private boolean aif;

    public MeshModel(MeshApplication context, short modelID, short destination, byte[] appKey) {
        this.modelID = modelID;
        this.mContext = context;
        this.mDestination = destination;
        procs = new ArrayList<>();
        if (null == appKey) {
            aif = false;
            mAppKey = getNetwork().getDeviceKey(destination);
        } else {
            aif = true;
            mAppKey = appKey;
        }
    }

    public MeshModel(short modelID, short vendorID) {
        this.modelID = modelID;
        this.vendorID = vendorID;
        procs = new ArrayList<>();
    }

    public MeshProcedure procedure(String procName) {
        for (MeshProcedure procedure : procs) {
            if (procedure.getName().equals(procName)) return procedure;
        }
        return null;
    }

    protected void addProcedure(MeshProcedure procedure) {
        procs.add(procedure);
    }

    public short getModelID() {
        return modelID;
    }

    public short getVendorID() {
        return vendorID;
    }

    public MeshNetwork getNetwork() {
        return mContext.getManager().getCurrentNetwork();
    }

    public MeshTransportLayer getTransportLayer() {
        return mContext.getTransportLayer();
    }

    public short getDestination() {
        return mDestination;
    }

    public boolean getAIF() {
        return aif;
    }

    public byte[] getAppKey() {
        return mAppKey;
    }

    public MeshApplication getApplicationContext() {
        return mContext;
    }
}
