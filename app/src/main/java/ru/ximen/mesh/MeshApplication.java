package ru.ximen.mesh;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class MeshApplication extends Application {
    private MeshManager manager;
    private MeshService mService;
    private MeshTransportLayer transportLayer;
    private MeshUpperTransportLayer upperTransportLayer;

    @Override
    public void onCreate() {
        super.onCreate();
        manager = new MeshManager(this, getFilesDir());
        transportLayer = new MeshTransportLayer(this);
        upperTransportLayer = new MeshUpperTransportLayer(this);
        Intent intent = new Intent(this, MeshService.class);
        if (bindService(intent, mConnection, BIND_AUTO_CREATE)) {
            Log.e("Application", "Error binding service");
        }
    }

    public MeshService getMeshService() {
        return mService;
    }

    public MeshManager getManager() {
        return manager;
    }

    public MeshTransportLayer getTransportLayer() {
        return transportLayer;
    }

    public MeshUpperTransportLayer getUpperTransportLayer() {
        return upperTransportLayer;
    }

    public ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            MeshService.LocalBinder binder = (MeshService.LocalBinder) service;
            mService = binder.getService();
        }

        public void onServiceDisconnected(ComponentName className) {

        }
    };
}

