package ru.ximen.meshstack;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

/**
 * <p>Service class maintaining bluetooth connections. It handles basic scan, connect, disconnect actions
 * and specific functions for individual bluetooth characteristics.</p>
 * <p>After starting and {@link #connect connecting}this service you need to {@link #registerCallback register}
 * callback for characteristic changes notifications. Then you can write to {@link #writeProvision(byte[]) provision} and
 * {@link #writeProxy(byte[]) proxy} characteristic.</p>
 * <p>Appropriate Intents called to notify connection and disconnection events.</p>
 *
 * @author Sergey Novgorodov
 */
public class MeshBluetoothService extends Service {
    /**
     * MTU size for bluetooth connection.
     */
    static int MTU = 20;
    /**
     * Default timeout for scanning neighbour bluetooth devices.
     */
    final static public int DEFAULT_SCAN_TIMEOUT = 10000;
    final static private int DEFAULT_RETRY_COUNT = 3;
    private int mConnectionState = STATE_DISCONNECTED;      // Initial connection state
    private final IBinder mBinder = new LocalBinder();      // Service binder for methods access
    private int retryCount;

    /**
     * UUID for provision DATA_IN characteristic.
     */
    final static public UUID MESH_PROVISION_DATA_IN = UUID.fromString("00002adb-0000-1000-8000-00805f9b34fb");
    /**
     * UUID for provision DATA_OUT characteristic.
     */
    final static public UUID MESH_PROVISION_DATA_OUT = UUID.fromString("00002adc-0000-1000-8000-00805f9b34fb");
    /**
     * UUID for proxy DATA_IN characteristic.
     */
    final static public UUID MESH_PROXY_DATA_IN = UUID.fromString("00002add-0000-1000-8000-00805f9b34fb");
    /**
     * UUID for proxy DATA_OUT characteristic.
     */
    final static public UUID MESH_PROXY_DATA_OUT = UUID.fromString("00002ade-0000-1000-8000-00805f9b34fb");
    /**
     * UUID to discover Proxy Service.
     */
    final static public UUID MESH_PROXY_SERVICE = UUID.fromString("00001828-0000-1000-8000-00805f9b34fb");
    /**
     * UUID to discover Provision Service.
     */
    final static public UUID MESH_PROVISION_SERVICE = UUID.fromString("00001827-0000-1000-8000-00805f9b34fb");
    /**
     * UUID to discover Configuration Client.
     */
    final static public UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * Intent action for establishing connection notification
     */
    public final static String ACTION_GATT_CONNECTED = "ru.ximen.meshstack.le.ACTION_GATT_CONNECTED";     // Intent for connected event
    /**
     * Intent action for disconnection notification
     */
    public final static String ACTION_GATT_DISCONNECTED = "ru.ximen.meshstack.le.ACTION_GATT_DISCONNECTED";  // Intent for disconnected event
    /**
     * Intent action for reconnecting GATT notification
     */
    public final static String ACTION_GATT_RECONNECTING = "ru.ximen.meshstack.le.ACTION_GATT_RECONNECTING";     // Intent for connected event
    final static private String TAG = MeshBluetoothService.class.getSimpleName();

    private BluetoothGattCharacteristic mProvisionCharacteristic;
    private BluetoothGattCharacteristic mProxyCharacteristic;
    private BluetoothGatt mBluetoothGatt;
    private LocalBroadcastManager mBroadcastManager;
    private HashMap<UUID, MeshCharacteristicChangedCallback> mCharacteristicCallbackMap;

    /**
     * Binder class to bind service so clients can call public methods.
     */
    class LocalBinder extends Binder {
        /**
         * Gets instance object for service connection.
         *
         * @return the service instance object
         */
        MeshBluetoothService getService() {
            return MeshBluetoothService.this;
        }
    }

    /**
     * Callback interface to notify bluetooth characteristic changed.
     */
    interface MeshCharacteristicChangedCallback {
        /**
         * Method called when characteristic changes (new data received). <b>data</b> parameter
         * contains received data.
         *
         * @param data received data
         */
        void onCharacteristicChanged(byte[] data);
    }

    /**
     * Method to register callbacks on bluetooth characteristic changes.
     *
     * @param characteristic UUID of characteristic
     * @param callback       callback function
     */
    void registerCallback(UUID characteristic, MeshCharacteristicChangedCallback callback){
        mCharacteristicCallbackMap.put(characteristic, callback);
    }

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    String intentAction = "";
                    if (status == GATT_SUCCESS) {
                        retryCount = 0;
                        if (newState == STATE_CONNECTED) {
                            intentAction = ACTION_GATT_CONNECTED;
                            mConnectionState = STATE_CONNECTED;
                            Log.i(TAG, "Connected to GATT server.");
                            mBluetoothGatt.discoverServices();
                        } else {
                            intentAction = ACTION_GATT_DISCONNECTED;
                            mConnectionState = STATE_DISCONNECTED;
                            gatt.close();
                            Log.i(TAG, "Disconnected from GATT server.");
                        }
                        broadcastUpdate(intentAction);
                    } else {
                        if(--retryCount > 0){
                            Log.e(TAG, "GATT not SUCCESS. Retrying connect");
                            intentAction = ACTION_GATT_RECONNECTING;
                            broadcastUpdate(intentAction);
                            gatt.connect();
                        } else gatt.close();
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    MeshCharacteristicChangedCallback callback = mCharacteristicCallbackMap.get(characteristic.getUuid());
                    if (callback != null) callback.onCharacteristicChanged(characteristic.getValue());
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        Log.d(TAG, service.getUuid().toString());
                    }
                    BluetoothGattService srvProxy = gatt.getService(MESH_PROXY_SERVICE);
                    BluetoothGattService srvProvision = gatt.getService(MESH_PROVISION_SERVICE);
                    if (null != srvProvision) {
                        // Set characteristic change notification
                        Log.v(TAG, "Subscribing characteristics notifications");
                        BluetoothGattCharacteristic provCharacteristic = srvProvision.getCharacteristic(MESH_PROVISION_DATA_OUT);
                        gatt.setCharacteristicNotification(provCharacteristic, true);
                        BluetoothGattDescriptor provDescriptor = provCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        provDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(provDescriptor);
                        mProvisionCharacteristic = srvProvision.getCharacteristic(MESH_PROVISION_DATA_IN);
                    } else if (null == srvProxy) {
                        Log.e(TAG, "No provision nor proxy service found");
                        disconnect();
                        return;
                    } else {
                        // Set characteristic change notification
                        Log.v(TAG, "Subscribing characteristics notifications");
                        BluetoothGattCharacteristic proxyCharacteristic = srvProxy.getCharacteristic(MESH_PROXY_DATA_OUT);
                        gatt.setCharacteristicNotification(proxyCharacteristic, true);
                        BluetoothGattDescriptor proxyDescriptor = proxyCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        proxyDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(proxyDescriptor);
                        mProxyCharacteristic = srvProxy.getCharacteristic(MESH_PROXY_DATA_IN);
                    }
                    //gatt.requestMtu(70);
                    //Log.i(TAG, "Requesting MTU change");
                    //gatt.requestMtu(70);
                }

                @Override
                public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                    super.onMtuChanged(gatt, mtu, status);
                    Log.i(TAG, "MTU is " + mtu);
                    MTU = mtu;
                }
            };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        mBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Writes data to bluetooth characteristic {@link #MESH_PROVISION_DATA_IN}.
     *
     * @param data data to be written
     */
    void writeProvision(byte[] data) {
        mProvisionCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mProvisionCharacteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(mProvisionCharacteristic);
    }

    /**
     * Writes data to bluetooth characteristic {@link #MESH_PROXY_DATA_IN}.
     *
     * @param data data to be written
     */
    void writeProxy(byte[] data) {
        mProxyCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mProxyCharacteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(mProxyCharacteristic);
    }

    /**
     * Connects unprovisioned bluetooth device.
     *
     * @param device BluetoothDevice to connect
     */
    public void connect(BluetoothDevice device) {
        Log.d(TAG, "Connecting device " + device.toString());
        if(retryCount == 0) retryCount = DEFAULT_RETRY_COUNT;
        mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
    }

    /**
     * Connects provisioned {@link MeshDevice}.
     *
     * @param device MeshDevice to connect
     */
    public void connect(MeshDevice device) {
        Log.d(TAG, "Connecting device " + device.getName());
        BluetoothDevice bDevice = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getRemoteDevice(device.getMAC());
        connect(bDevice);
    }

    /**
     * Disconnect from currently connected device.
     */
    public void disconnect() {
        if (mConnectionState == STATE_CONNECTED) {
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * Scan neighbour bluetooth devices available to connect. Scan stops after {@link #DEFAULT_SCAN_TIMEOUT} seconds.
     * Requires BLUETOOTH_ADMIN permissions.
     *
     * @param scanCallback Callback function called on every device discovery
     */
    public void scan(final ScanCallback scanCallback) {
        final BluetoothLeScanner bluetoothScanner = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getBluetoothLeScanner();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothScanner.stopScan(scanCallback);
            }
        }, DEFAULT_SCAN_TIMEOUT);

        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(MeshBluetoothService.MESH_PROVISION_SERVICE)).build();
        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(scanFilter);
        ScanSettings scanSettings = new ScanSettings.Builder().build();
        bluetoothScanner.startScan(scanFilters, scanSettings, scanCallback);
    }

    /**
     * Stops scanning bluetooth devices.
     * Requires BLUETOOTH_ADMIN permissions.
     *
     * @param scanCallback Callback function
     */
    public void stopScan(ScanCallback scanCallback){
        BluetoothLeScanner bluetoothScanner = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getBluetoothLeScanner();
        bluetoothScanner.stopScan(scanCallback);
    }

    /**
     * Returns currently connected BluetoothDevice.
     *
     * @return the connected device
     */
    public BluetoothDevice getConnectedDevice() {
        if (mConnectionState == STATE_CONNECTED) {
            return mBluetoothGatt.getDevice();
        } else return null;
    }

    /**
     * Returns current connection state: <b>true</b> if connected, else <b>false</b>.
     *
     * @return connected state
     */
    public boolean isConnected() {
        return (mConnectionState == STATE_CONNECTED);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        mCharacteristicCallbackMap = new HashMap<>();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}

