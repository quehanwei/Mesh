package ru.ximen.mesh;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
    private LocalBroadcastManager mBroadcastManager;
    private boolean mConnected;
    private boolean mBound;
    private MeshNetwork mNetwork;

    final static private String TAG = "BluetoothMesh";
    final static private int DEFAULT_SCAN_TIMEOUT = 10000;

    public ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.v(TAG,"connected");
            MeshService.LocalBinder binder = (MeshService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }
        public void onServiceDisconnected(ComponentName className) {
            Log.v(TAG,"disconnected");
            mBound = false;
        }
    };

    public static synchronized BluetoothMesh getInstance(Context context, String jsonNetwork) {
        if(null == mInstance){
            mInstance = new BluetoothMesh(context, jsonNetwork);
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
    protected BluetoothMesh(Context context, String networkName) {
        mConnected = false;
        mBound = false;
        mContext = context;
        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mScanResult = new ArrayList<>();
        mHandler = new Handler();
        setScanTimeout(DEFAULT_SCAN_TIMEOUT);      // Default value
        Log.d(TAG, "Binding service");
        Intent intent = new Intent(mContext, MeshService.class);
        if (!mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "Error binding service");
            mBound = false;
        }
        IntentFilter filter = new IntentFilter(MeshService.ACTION_GATT_CONNECTED);
        filter.addAction(MeshService.ACTION_GATT_DISCONNECTED);
        proxy = new MeshProxyModel(mContext, mBluetoothGatt);
        provisioner = new MeshProvisionModel(mContext, proxy);
        mBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        mBroadcastManager.registerReceiver(mGattUpdateReceiver, filter);
        Log.d(TAG, "Network name: " + networkName);
        mNetwork = ((MeshApplication) mContext.getApplicationContext()).getManager().getNetwork(networkName);
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

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.writeCharacteristic(characteristic);
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
                Log.d(TAG, "Scan result size: " + mScanResult.size());
                finishCallback.finished(mScanResult);
            }
        }, mScanTimeout);

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(MeshService.MESH_PROVISION_SERVICE))
                .build();
        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(scanFilter);

        ScanSettings scanSettings =
                new ScanSettings.Builder().build();

        mBluetoothScanner.startScan(scanFilters, scanSettings, mScanCallback);
    }

    private ScanCallback mScanCallback = new ScanCallback() {
                public void onScanResult(int callbackType, ScanResult result) {
                    Log.v(TAG, result.toString());
                    //boolean duplicate = false;
                    //for (ScanResult item : mScanResult)
                    //    if (result.getDevice().equals(item.getDevice())) duplicate = true;
                    //if (!duplicate) {
                        mScanResult.add(result);
                    //}
                }
            };

    public void connect(BluetoothDevice device){
        Log.d(TAG, "Connecting device " + device.toString());
        mBluetoothGatt = device.connectGatt(mContext, false, mService.mGattCallback);
    }

    public void disconnect(){
        if (mConnected) mBluetoothGatt.disconnect();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MeshService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Log.d(TAG, "Iterating services list:");
                mBluetoothGatt.discoverServices();
            } else if (MeshService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
            }
        }
    };

    // Creates new network with all necessary keys
    public void createNetwork() {

    }

    // Provisions unprovisioned device
    public void provisionDevice(final MeshProvisionModel.MeshProvisionCallback provisionCallback) {
        provisioner.startProvision(provisionCallback);
    }

    protected MeshService getService() {
        return mService;
    }

    public void close(){
        Log.d(TAG, "Closing GATT");
        disconnect();
        Log.d(TAG, "Unbinding service");
        if (mBound) {
            mService.stopSelf();
            mContext.unbindService(mConnection);
        }
        mBroadcastManager.unregisterReceiver(mGattUpdateReceiver);
        provisioner.close();
        proxy.close();
    }

    public MeshNetwork getNetwork() {
        return mNetwork;
    }

    public BluetoothDevice getDevice() {
        return mBluetoothGatt.getDevice();
    }
}
