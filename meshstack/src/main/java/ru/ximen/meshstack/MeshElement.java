package ru.ximen.meshstack;

import java.util.ArrayList;

public class MeshElement {
    class Model{
        byte[] id;
        byte aid;

        public Model(byte aid, byte[] id) {
            this.aid = aid;
            this.id = id;
        }

        public byte[] getId() {
            return id;
        }

        public byte[] getAid() {
            return id;
        }
    }


    private short descriptor;
    private String name;
    private short address;
    private ArrayList<Model> models;

    public MeshElement(String name, short address) {
        this.name = name;
        this.address = address;
        models = new ArrayList<>();
    }

    void addModel(Model model){
        models.add(model);
    }

    public String getName() {
        return name;
    }

    public short getAddress() {
        return address;
    }

    public ArrayList<Model> getModels() {
        return models;
    }

    Model newModel(byte aid, byte[] id){
        return  new Model(aid, id);
    }

    public void setDescriptor(short descriptor){
        this.descriptor = descriptor;
    }
}
