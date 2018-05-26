package ru.ximen.mesh;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private ArrayList<String> listItems=new ArrayList<>();
    private ArrayAdapter<String> mLstAdapter;
    private BluetoothMesh mesh;
    private ArrayMap<String, BluetoothDevice> mDeviceMap;
    final static private String TAG = "Mesh";
    private LocalBroadcastManager mBroadcastManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.loadingPanel).setVisibility(View.INVISIBLE);
        final ListView listview = findViewById(R.id.list);
        mLstAdapter=new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        listview.setAdapter(mLstAdapter);

        toolbar = findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

//        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {
//            // Device doesn't support Bluetooth
//        }
//        if (!mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }

        mesh = BluetoothMesh.getInstance(getApplicationContext(), getIntent().getStringExtra("ru.ximen.mesh.NETWORK"));
        mDeviceMap = new ArrayMap<>();
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                Toast.makeText(getApplicationContext(), "Connecting device", Toast.LENGTH_LONG).show();
                mesh.connect(mDeviceMap.get(item));
            }

        });
        mBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_scan) {
            mesh.scan(new BluetoothMesh.MeshScanCallback() {
                @Override
                public void finished(ArrayList<ScanResult> scanResults) {
                    mDeviceMap.clear();
                    listItems.clear();
                    for (ScanResult item : scanResults) {
                        mDeviceMap.put(item.getDevice().getAddress(), item.getDevice());
                        listItems.add(item.getDevice().getAddress());
                        Log.d(TAG, "Scan result: " + item.getDevice().getAddress());
                    }
                    mLstAdapter.notifyDataSetChanged();

                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                }
            });
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        }

        return super.onOptionsItemSelected(item);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MeshService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "Starting device activity");
                Toast.makeText(context, "Device connected", Toast.LENGTH_SHORT);
                Intent newIntent = new Intent(context, DeviceActivity.class);
                startActivity(newIntent);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MeshService.ACTION_GATT_CONNECTED);
        mBroadcastManager.registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBroadcastManager.unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        mesh.close();
        super.onDestroy();
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == BluetoothHelper.BLUETOOTH_REQUEST) {
            if (resultCode == RESULT_CANCELED) finish();
        }
    }*/

}
