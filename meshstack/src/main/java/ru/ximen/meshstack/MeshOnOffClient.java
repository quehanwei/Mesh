package ru.ximen.meshstack;

public class MeshOnOffClient extends MeshClient {
    public MeshOnOffClient(MeshApplication context, short destination) {
        super(context, destination);
        addModel(new MeshOnOffModel(context, destination));
    }
}

