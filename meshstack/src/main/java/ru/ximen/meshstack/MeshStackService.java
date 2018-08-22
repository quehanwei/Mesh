package ru.ximen.meshstack;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * The MeshStackService handles all Bluetooth Mesh stack and gains access to it's functions.
 * Also givess access to {@link MeshBluetoothService} for low level bluetooth actions and
 * {@link MeshManager} and {@link MeshAppManager} to manage networks and applications.
 *
 * @author Sergey Novgorodov
 */
public class MeshStackService extends Service {
    private final IBinder mBinder = new LocalBinder();
    private MeshBluetoothService mBluetoothService;
    private LocalBroadcastManager mBroadcastManager;
    private MeshManager mManager;
    private MeshProxyModel mProxy;
    private MeshTransportLayer mTransportLayer;
    private MeshUpperTransportLayer mUpperTransportLayer;
    private MeshAppManager mAppManager;


    /**
     * Binder class to bind service so clients can call public methods.
     */
    public class LocalBinder extends Binder {
        /**
         * Gets instance object for service connection.
         *
         * @return the service instance object
         */
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
        IntentFilter filter = new IntentFilter(MeshBluetoothService.ACTION_GATT_CONNECTED);
        filter.addAction(MeshBluetoothService.ACTION_GATT_DISCONNECTED);
        mBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        mBroadcastManager.registerReceiver(mConnectionStateReceiver, filter);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            MeshBluetoothService.LocalBinder binder = (MeshBluetoothService.LocalBinder) service;
            mBluetoothService = binder.getService();
        }

        public void onServiceDisconnected(ComponentName className) {}
    };

    /**
     * Gets access to {@link MeshBluetoothService} service functions.
     *
     * @return the mesh bluetooth service
     */
    public MeshBluetoothService getMeshBluetoothService() {
        return mBluetoothService;
    }

    /**
     * Returns {@link MeshManager} object handling storage and use of network objects.
     *
     * @return the network manager
     */
    public MeshManager getNetworkManager() {
        return mManager;
    }

    /**
     * Returns Application manager that manages Application keys for Bluetooth Mesh network.
     *
     * @return the application manager object
     */
    public MeshAppManager getAppManager() {
        return mAppManager;
    }

    /**
     * Returns Transport Layer of Bluetooth Mesh.
     *
     * @return the Transport Layer object
     */
    MeshTransportLayer getTransportLayer() {
        return mTransportLayer;
    }

    /**
     * Returns Upper Transport Layer of Bluetooth Mesh.
     *
     * @return the Upper Transport Layer object
     */
    MeshUpperTransportLayer getUpperTransportLayer() {
        return mUpperTransportLayer;
    }

    /**
     * Returns {@link MeshProxyModel} object representing Proxy Model of Bluetooth Mesh.
     *
     * @return the Proxy Model object
     */
    MeshProxyModel getProxy() {
        return mProxy;
    }

    BroadcastReceiver mConnectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == MeshBluetoothService.ACTION_GATT_CONNECTED){
                mProxy = new MeshProxyModel(MeshStackService.this);
            } else if(action == MeshBluetoothService.ACTION_GATT_DISCONNECTED) {
                mProxy = null;
            }
        }
    };
}
