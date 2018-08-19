package ru.ximen.meshstack;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class MeshStackService extends Service {
    private final IBinder mBinder = new LocalBinder();
    private MeshBluetoothService mBluetoothService;

    private MeshManager mManager;
    private MeshTransportLayer mTransportLayer;
    private MeshUpperTransportLayer mUpperTransportLayer;
    private MeshAppManager mAppManager;


    public class LocalBinder extends Binder {
        public MeshStackService getService() {
            return MeshStackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Binding bluetooth service
        Intent intent = new Intent(this, MeshBluetoothService.class);
        if (bindService(intent, mConnection, BIND_AUTO_CREATE)) {
            Log.e("Application", "Error binding service");
        }
        // Initializing stack objects
        mManager = new MeshManager(this, getFilesDir());
        mTransportLayer = new MeshTransportLayer(this);
        mUpperTransportLayer = new MeshUpperTransportLayer(this);
        mAppManager = new MeshAppManager();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            MeshBluetoothService.LocalBinder binder = (MeshBluetoothService.LocalBinder) service;
            mBluetoothService = binder.getService();
        }

        public void onServiceDisconnected(ComponentName className) {}
    };

    public MeshBluetoothService getMeshService() {
        return mBluetoothService;
    }

    public MeshManager getNetworkManager() {
        return mManager;
    }

    public MeshAppManager getAppManager() {
        return mAppManager;
    }

    public MeshTransportLayer getTransportLayer() {
        return mTransportLayer;
    }

    public MeshUpperTransportLayer getUpperTransportLayer() {
        return mUpperTransportLayer;
    }
}
