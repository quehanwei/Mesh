package ru.ximen.mesh;

import android.app.Application;

public class MeshApplication extends Application {
    private MeshManager manager;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = new MeshManager(this, getFilesDir());
    }

    public MeshManager getManager() {
        return manager;
    }
}
