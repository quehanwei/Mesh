package ru.ximen.meshstack;

public class MeshOnOffClient extends MeshClient {
    public MeshOnOffClient(MeshStackService context, short destination) {
        super(context, destination);
        addModel(new MeshOnOffModel(context, destination));
    }
}

