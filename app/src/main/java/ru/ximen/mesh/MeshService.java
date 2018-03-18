package ru.ximen.mesh;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

public class MeshService extends Service {
    private int mConnectionState = STATE_DISCONNECTED;      // Initial connection state
    private final IBinder mBinder = new LocalBinder();      // Service binder for methods access

    public final static String ACTION_GATT_CONNECTED =    "ru.ximen.mesh.le.ACTION_GATT_CONNECTED";     // Intent for connected event
    public final static String ACTION_GATT_DISCONNECTED = "ru.ximen.mesh.le.ACTION_GATT_DISCONNECTED";  // Intent for disconnected event
    public final static String ACTION_PROXY_DATA_AVAILABLE =    "ru.ximen.mesh.le.ACTION_PROXY_DATA_AVAILABLE";     // Intent for new data available
    public final static String ACTION_PROVISION_DATA_AVAILABLE =    "ru.ximen.mesh.le.ACTION_PROVISION_DATA_AVAILABLE";     // Intent for new data available
    public final static String EXTRA_DATA = "ru.ximen.mesh.le.EXTRA_DATA";
    final static private String TAG = "MeshService";

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
                    if (characteristic.getUuid().equals(BluetoothMesh.MESH_PROXY_DATA_IN)){
                        broadcastUpdate(ACTION_PROXY_DATA_AVAILABLE, characteristic);
                    } else if (characteristic.getUuid().equals(BluetoothMesh.MESH_PROVISION_DATA_IN)) {
                        broadcastUpdate(ACTION_PROVISION_DATA_AVAILABLE, characteristic);
                    }
                    Log.i(TAG, "Characteristic " + characteristic.toString() + " changed");
                }
                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    List<BluetoothGattService> services = gatt.getServices();
                    for (BluetoothGattService service : services){
                        Log.d(TAG, service.getUuid().toString());
                    }
                    //BluetoothGattService srvProxy = gatt.getService(BluetoothMesh.MESH_PROXY_SERVICE);
                    BluetoothGattService srvProvision = gatt.getService(BluetoothMesh.MESH_PROVISION_SERVICE);
                    if(null != srvProvision){
                        // Set characteristic change notification
                        Log.v(TAG, "Subscribing characteristics notifications");
                        BluetoothGattCharacteristic provCharacteristic = srvProvision.getCharacteristic(BluetoothMesh.MESH_PROVISION_DATA_OUT);
                        gatt.setCharacteristicNotification(provCharacteristic, true);
                        BluetoothGattDescriptor provDescriptor = provCharacteristic.getDescriptor(BluetoothMesh.CLIENT_CHARACTERISTIC_CONFIG);
                        provDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(provDescriptor);
                    } else {
                        Log.e(TAG, "No provision service found");
                    }
                    //BluetoothGattCharacteristic proxyCharacteristic = srvProxy.getCharacteristic(BluetoothMesh.MESH_PROXY_DATA_OUT);
                    //gatt.setCharacteristicNotification(proxyCharacteristic, true);
                    //BluetoothGattDescriptor proxyDescriptor = proxyCharacteristic.getDescriptor(BluetoothMesh.CLIENT_CHARACTERISTIC_CONFIG);
                    //proxyDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    //gatt.writeDescriptor(proxyDescriptor);


                }
            };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }   Log.d(TAG, new String(data));
        sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}

