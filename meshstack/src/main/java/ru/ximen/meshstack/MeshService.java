package ru.ximen.meshstack;

import android.app.Service;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class MeshService extends Service {
    protected static int MTU = 20;
    final static public int DEFAULT_SCAN_TIMEOUT = 10000;
    private int mConnectionState = STATE_DISCONNECTED;      // Initial connection state
    private final IBinder mBinder = new LocalBinder();      // Service binder for methods access

    final static public UUID MESH_PROVISION_DATA_IN = UUID.fromString("00002adb-0000-1000-8000-00805f9b34fb");
    final static public UUID MESH_PROVISION_DATA_OUT = UUID.fromString("00002adc-0000-1000-8000-00805f9b34fb");
    final static public UUID MESH_PROXY_DATA_IN = UUID.fromString("00002add-0000-1000-8000-00805f9b34fb");
    final static public UUID MESH_PROXY_DATA_OUT = UUID.fromString("00002ade-0000-1000-8000-00805f9b34fb");
    final static public UUID MESH_PROXY_SERVICE = UUID.fromString("00001828-0000-1000-8000-00805f9b34fb");
    final static public UUID MESH_PROVISION_SERVICE = UUID.fromString("00001827-0000-1000-8000-00805f9b34fb");
    final static public UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public final static String ACTION_GATT_CONNECTED = "ru.ximen.mesh.le.ACTION_GATT_CONNECTED";     // Intent for connected event
    public final static String ACTION_GATT_DISCONNECTED = "ru.ximen.mesh.le.ACTION_GATT_DISCONNECTED";  // Intent for disconnected event
    public final static String ACTION_PROXY_DATA_AVAILABLE = "ru.ximen.mesh.le.ACTION_PROXY_DATA_AVAILABLE";     // Intent for new data available
    public final static String ACTION_PROVISION_DATA_AVAILABLE = "ru.ximen.mesh.le.ACTION_PROVISION_DATA_AVAILABLE";     // Intent for new data available
    public final static String ACTION_PROXY_CONFIGURATION_DATA_AVAILABLE = "ru.ximen.mesh.le.ACTION_PROXY_CONFIGURATION_DATA_AVAILABLE";     // Intent for new data available
    public final static String ACTION_MESH_BEACON_DATA_AVAILABLE = "ru.ximen.mesh.le.ACTION_MESH_BEACON_DATA_AVAILABLE";     // Intent for new data available
    public final static String ACTION_NETWORK_DATA_AVAILABLE = "ru.ximen.mesh.le.ACTION_NETWORK_DATA_AVAILABLE";     // Intent for new data available
    public final static String ACTION_TRANSPORT_DATA_AVAILABLE = "ru.ximen.mesh.le.ACTION_TRANSPORT_DATA_AVAILABLE";     // Intent for new data available
    public static final String ACTION_UPPER_TRANSPORT_DATA_AVAILABLE = "ru.ximen.mesh.le.ACTION_UPPER_TRANSPORT_DATA_AVAILABLE";     // Intent for new data available
    public final static String ACTION_APPLICATION_DATA_AVAILABLE = "ru.ximen.mesh.le.ACTION_APPLICATION_DATA_AVAILABLE_";     // Intent for new data available
    public final static String EXTRA_DATA = "ru.ximen.mesh.le.EXTRA_DATA";
    public final static String EXTRA_ADDR = "ru.ximen.mesh.le.EXTRA_ADDR";
    public final static String EXTRA_SEQ = "ru.ximen.mesh.le.EXTRA_SEQ";
    final static private String TAG = "MeshService";

    private BluetoothGattCharacteristic mProvisionCharacteristic;
    private BluetoothGattCharacteristic mProxyCharacteristic;
    private BluetoothGatt mBluetoothGatt;
    private LocalBroadcastManager mBroadcastManager;

    public class LocalBinder extends Binder {
        MeshService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MeshService.this;
        }
    }

    public final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    String intentAction;
                    if (status == GATT_SUCCESS) {
                        if (newState == STATE_CONNECTED) {
                            intentAction = ACTION_GATT_CONNECTED;
                            mConnectionState = STATE_CONNECTED;
                            broadcastUpdate(intentAction);
                            Log.i(TAG, "Connected to GATT server.");
                            mBluetoothGatt.discoverServices();
                        } else if (newState == STATE_DISCONNECTED) {
                            intentAction = ACTION_GATT_DISCONNECTED;
                            mConnectionState = STATE_DISCONNECTED;
                            broadcastUpdate(intentAction);
                            gatt.close();
                            Log.i(TAG, "Disconnected from GATT server.");
                        }
                    } else gatt.close();
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    if (characteristic.getUuid().equals(MESH_PROXY_DATA_OUT)) {
                        broadcastUpdate(ACTION_PROXY_DATA_AVAILABLE, characteristic);
                        Log.d(TAG, "Proxy characteristic changed");
                    } else if (characteristic.getUuid().equals(MESH_PROVISION_DATA_OUT)) {
                        broadcastUpdate(ACTION_PROXY_DATA_AVAILABLE, characteristic);
                        Log.d(TAG, "Provision characteristic changed");
                    }
                    //Log.d(TAG, "Characteristic " + characteristic.getUuid().toString() + " changed");
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services) {
                        //Log.d(TAG, service.getUuid().toString());
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

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        Log.d(TAG, "Data: " + Utils.toHexString(data));
        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA, data);
        }
        mBroadcastManager.sendBroadcast(intent);
    }

    protected void writeProvision(byte[] params) {
        mProvisionCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mProvisionCharacteristic.setValue(params);
        writeCharacteristic(mProvisionCharacteristic);
    }

    protected void writeProxy(byte[] params) {
        mProxyCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mProxyCharacteristic.setValue(params);
        writeCharacteristic(mProxyCharacteristic);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void connect(BluetoothDevice device) {
        Log.d(TAG, "Connecting device " + device.toString());
        mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
    }

    public void connect(MeshDevice device) {
        Log.d(TAG, "Connecting device " + device.getName());
        BluetoothDevice bDevice = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getRemoteDevice(device.getMAC());
        mBluetoothGatt = bDevice.connectGatt(getApplicationContext(), false, mGattCallback);
    }

    public void disconnect() {
        if (mConnectionState == STATE_CONNECTED) {
            mBluetoothGatt.disconnect();
        }
    }

    public void scan(final ScanCallback scanCallback) {
        final BluetoothLeScanner bluetoothScanner = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getBluetoothLeScanner();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothScanner.stopScan(scanCallback);
            }
        }, DEFAULT_SCAN_TIMEOUT);

        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(MeshService.MESH_PROVISION_SERVICE)).build();
        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(scanFilter);
        ScanSettings scanSettings = new ScanSettings.Builder().build();
        bluetoothScanner.startScan(scanFilters, scanSettings, scanCallback);
    }

    public BluetoothDevice getConnectedDevice() {
        if (mConnectionState == STATE_CONNECTED) {
            return mBluetoothGatt.getDevice();
        } else return null;
    }

    public boolean isConnected() {
        return (mConnectionState == STATE_CONNECTED);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}

