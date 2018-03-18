package ru.ximen.mesh;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

/**
 * Class BluetoothMesh used for operations within Bluetooth Mesh network. Contains Proxy and Provision models
 */
public class BluetoothMesh {
    private static BluetoothMesh mInstance = null;
    private MeshProxyModel proxy;                   // Proxy model object
    private MeshProvisionModel provisioner;       // Provision model object

    private BluetoothLeScanner mBluetoothScanner;   // Bluetooth scanner for scan devices
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;                       // Handler to handle runnable in scan delay
    private long mScanTimeout;                       // Scan period
    private ArrayList<ScanResult> mScanResult;      // Result of scanning devices
    private Context mContext;                       // Application context
    private BluetoothGatt mBluetoothGatt;           // GATT for sending and receiving messages
    private MeshService mService;                   // Service for bluetooth operations

    final static public UUID MESH_PROVISION_DATA_IN   = UUID.fromString("00002adb-0000-1000-8000-00805f9b34fb");
    final static public UUID MESH_PROVISION_DATA_OUT   = UUID.fromString("00002adc-0000-1000-8000-00805f9b34fb");
    final static public UUID MESH_PROXY_DATA_IN   = UUID.fromString("00002add-0000-1000-8000-00805f9b34fb");
    final static public UUID MESH_PROXY_DATA_OUT   = UUID.fromString("00002ade-0000-1000-8000-00805f9b34fb");
    final static public UUID MESH_PROXY_SERVICE   = UUID.fromString("00001828-0000-1000-8000-00805f9b34fb");
    final static public UUID MESH_PROVISION_SERVICE   = UUID.fromString("00001827-0000-1000-8000-00805f9b34fb");
    final static public UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    final static private String TAG = "BluetoothMesh";

    public ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.v(TAG,"connected");
            MeshService.LocalBinder binder = (MeshService.LocalBinder) service;
            mService = binder.getService();
            //mBound = true;
        }
        public void onServiceDisconnected(ComponentName className) {
            Log.v(TAG,"disconnected");
            //mBound = false;
        }
    };

    public static synchronized BluetoothMesh getInstance(Context context){
        if(null == mInstance){
            mInstance = new BluetoothMesh(context);
        }
        return mInstance;
    }

    public static synchronized BluetoothMesh getInstance(){
        return mInstance;
    }

    /**
     * Constructor to initialize Bluetooth Mesh class
     *
     * @param context Application context
     */
    protected BluetoothMesh(Context context){
        mContext = context;
        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mScanResult = new ArrayList<>();
        mHandler = new Handler();
        setScanTimeout(10000);
        Log.d(TAG, "Binding service");
        Intent intent = new Intent(mContext, MeshService.class);
        if (!mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Error binding service");
        }
        IntentFilter filter = new IntentFilter(MeshService.ACTION_GATT_CONNECTED);
        filter.addAction(MeshService.ACTION_GATT_DISCONNECTED);
        context.registerReceiver(mGattUpdateReceiver, filter);
        proxy = new MeshProxyModel(mContext);
        provisioner = new MeshProvisionModel(mContext);
    }

    /**
     * Returns current device scan period value
     *
     * @return  scan period in milliseconds
     */
    public long getScanTimeout() {
        return mScanTimeout;
    }
    public void setScanTimeout(int mScanTimeout) {
        this.mScanTimeout = mScanTimeout;
    }

    public interface MeshScanCallback{
        void finished(ArrayList<ScanResult> scanResults);
    }
    // Scans for unprovisioned mesh devices
    // Returns List of BluetoothDevices matching all criteria (0x1812 service UUID)
    public void scan(final MeshScanCallback finishCallback) {
        mScanResult.clear();
        mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothScanner.stopScan(mScanCallback);
                finishCallback.finished(mScanResult);
            }
        }, mScanTimeout);

        mBluetoothScanner.startScan(mScanCallback);
    }

    private ScanCallback mScanCallback = new ScanCallback() {
                public void onScanResult(int callbackType, ScanResult result) {
                    Log.v(TAG, result.toString());
                    boolean duplicate = false;
                    for (ScanResult item : mScanResult)
                        if (result.getDevice().equals(item.getDevice())) duplicate = true;
                    if (!duplicate) {
                        mScanResult.add(result);
                    }
                }
            };

    public void connect(BluetoothDevice device){
        Log.d(TAG, "Connecting device " + device.toString());
        mBluetoothGatt = device.connectGatt(mContext, false, mService.mGattCallback);
    }

    public void disconnect(){
        mBluetoothGatt.disconnect();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MeshService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "Iterating services list:");
                mBluetoothGatt.discoverServices();
            } else if (MeshService.ACTION_GATT_DISCONNECTED.equals(action)) {

            }
        }
    };

    // Creates new network with all necessary keys
    public void createNetwork() {

    }

    // Provisions unprovisioned device
    public void provisionDevice(){
        provisioner.startProvision();
    }

    public void close(){
        Log.d(TAG, "Closing GATT");
        mBluetoothGatt.disconnect();
        Log.d(TAG, "Unbinding service");
        mContext.unbindService(mConnection);
        mContext.unregisterReceiver(mGattUpdateReceiver);
        provisioner.close();
        proxy.close();
    }
}
