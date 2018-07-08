package ru.ximen.mesh;

public class MeshOnOffModel extends MeshModel {
    public MeshOnOffModel(MeshApplication context, short destination) {
        super(context, MeshModel.ID_ONOFF_MODEL_CLIENT, destination, null); // Todo: set proper AppKey
        addProcedure(new MeshOnOffProc(this));
    }
}
