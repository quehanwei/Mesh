package ru.ximen.mesh;

/**
 * Created by ximen on 17.03.18.
 */

public abstract class MeshPDU {
    public abstract byte[] data();

    public abstract void setData(byte[] data);
}
