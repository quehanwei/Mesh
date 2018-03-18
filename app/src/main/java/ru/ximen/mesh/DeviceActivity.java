package ru.ximen.mesh;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class DeviceActivity extends AppCompatActivity {
    private Toolbar toolbar;
    BluetoothMesh mesh;
    final static private String TAG = "MeshProvision";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mesh = BluetoothMesh.getInstance();
        setContentView(R.layout.activity_device);
        toolbar = findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);
    }

    public void onProvisionButton(View view) {
        Log.i(TAG, "Starting provision");
        mesh.provisionDevice();
    }

    public void onUnProvisionButton(View view) {
        Log.i(TAG, "Starting unprovision");
    }

    public void onDisconnectButton(View view) {
        Log.i(TAG, "Disconnecting device");
        mesh.disconnect();
        finish();
    }
}
