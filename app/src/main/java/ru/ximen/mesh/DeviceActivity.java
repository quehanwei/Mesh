package ru.ximen.mesh;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

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
        mesh.provisionDevice(new MeshProvisionModel.MeshProvisionCallback() {
            @Override
            public void getOOB(final MeshProvisionModel.MeshOOBCallback oobCallback) {
                AlertDialog.Builder alert = new AlertDialog.Builder(DeviceActivity.this);
                alert.setTitle("Enter Public Key");
                alert.setMessage("OOB Key:");

                // Set an EditText view to get user input
                final EditText input = new EditText(DeviceActivity.this);
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        oobCallback.gotOOB(value);
                        return;
                    }
                });

                alert.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                return;
                            }
                        });
                alert.show();
            }
        });
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
