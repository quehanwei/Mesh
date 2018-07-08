package ru.ximen.mesh;

import java.util.HashSet;

public abstract class MeshClient {
    private HashSet<MeshModel> models;
    private MeshApplication mContext;
    private short DST;

    public MeshClient(MeshApplication context, short destination) {
        mContext = context;
        DST = destination;
        models = new HashSet<>();
    }

    protected void addModel(MeshModel model) {
        models.add(model);
    }

    public MeshModel getModel(short modelID) {
        for (MeshModel model : models) {
            if (model.getModelID() == modelID) return model;
        }
        return null;
    }

    public MeshModel getModel(short modelID, short vendorID) {
        for (MeshModel model : models) {
            if ((model.getModelID() == modelID) && (model.getVendorID() == vendorID)) return model;
        }
        return null;
    }
}
