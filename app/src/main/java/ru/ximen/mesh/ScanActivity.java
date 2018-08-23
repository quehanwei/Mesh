package ru.ximen.mesh;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;

import ru.ximen.meshstack.MeshDevice;
import ru.ximen.meshstack.MeshNetwork;
import ru.ximen.meshstack.MeshProvisionModel;
import ru.ximen.meshstack.MeshBluetoothService;
import ru.ximen.meshstack.MeshStackService;

public class ScanActivity extends BasicServiceActivty {
    private Toolbar toolbar;
    private ArrayList<String> listItems=new ArrayList<>();
    private ArrayAdapter<String> mLstAdapter;
    private ArrayMap<String, BluetoothDevice> mDeviceMap;
    final static private String TAG = "Mesh";
    private LocalBroadcastManager mBroadcastManager;
    private ArrayList<ScanResult> mScanResult;
    private Menu mScanMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, MeshStackService.class));
        setContentView(R.layout.activity_scan);
        findViewById(R.id.loadingPanel).setVisibility(View.INVISIBLE);
        mBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());

    }

    @Override
    protected void onServiceAttached(MeshStackService service) {
        super.onServiceAttached(service);
        final ListView listview = findViewById(R.id.list);
        mLstAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        listview.setAdapter(mLstAdapter);

        toolbar = findViewById(R.id.toolbar); // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);

        mDeviceMap = new ArrayMap<>();
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                Toast.makeText(getApplicationContext(), "Connecting device", Toast.LENGTH_LONG).show();
                mStackService.getMeshBluetoothService().connect(mDeviceMap.get(item));
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        mScanMenu = menu;
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
            scan();
        }

        return super.onOptionsItemSelected(item);
    }

    private void scan() {
        mStackService.getMeshBluetoothService().scan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                mDeviceMap.put(result.getDevice().getAddress(), result.getDevice());
                listItems.add(result.getDevice().getAddress());
                Log.d(TAG, "Scan result: " + result.getDevice().getAddress());
                mLstAdapter.notifyDataSetChanged();
            }
        });
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        if (null != mScanMenu) {
            mScanMenu.findItem(R.id.action_scan).setTitle(R.string.action_scanning);
            mScanMenu.findItem(R.id.action_scan).setEnabled(false);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                if (null != mScanMenu) {
                    mScanMenu.findItem(R.id.action_scan).setTitle(R.string.action_scan);
                    mScanMenu.findItem(R.id.action_scan).setEnabled(true);
                }
            }
        }, MeshBluetoothService.DEFAULT_SCAN_TIMEOUT);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MeshBluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG, "Starting device activity");
                Toast.makeText(context, "Device connected", Toast.LENGTH_SHORT);
                //Intent newIntent = new Intent(context, DeviceActivity.class);
                //startActivity(newIntent);
                AlertDialog.Builder alertName = new AlertDialog.Builder(ScanActivity.this);
                alertName.setTitle("Enter device name");
                alertName.setMessage("Name:");

                // Set an EditText view to get user input
                final EditText input = new EditText(ScanActivity.this);
                alertName.setView(input);
                alertName.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String name = input.getText().toString();

                        Log.i(TAG, "Starting provision");
                        mStackService.getNetworkManager().getCurrentNetwork().provisionDevice(mStackService.getMeshBluetoothService().getConnectedDevice(),
                                        name, oobCallback,
                                            new MeshProvisionModel.MeshProvisionFinishedOOBCallback() {
                                            @Override
                                            public void finished(MeshDevice device, MeshNetwork network) {
                                                finish();
                                            }
                                        });
                    }
                });

                alertName.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                mStackService.getMeshBluetoothService().disconnect();
                            }
                        });
                alertName.show();

            } else if (MeshBluetoothService.ACTION_GATT_RECONNECTING.equals(action)) {
                Toast.makeText(context, "Reconnecting...", Toast.LENGTH_LONG);
            }
        }
    };

    MeshProvisionModel.MeshProvisionGetOOBCallback oobCallback = new MeshProvisionModel.MeshProvisionGetOOBCallback() {
        @Override
        public void getOOB(final MeshProvisionModel.MeshProvisionOOBCallback callback) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder alert = new AlertDialog.Builder(ScanActivity.this);
                    alert.setTitle("Enter Public Key");
                    alert.setMessage("OOB Key:");

                    // Set an EditText view to get user input
                    final EditText input = new EditText(ScanActivity.this);
                    alert.setView(input);

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input.getText().toString();
                            callback.gotOOB(value);
                            return;
                        }
                    });

                    alert.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    mStackService.getMeshBluetoothService().disconnect();
                                    return;
                                }
                            });
                    alert.show();
                }
            });
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MeshBluetoothService.ACTION_GATT_CONNECTED);
        filter.addAction(MeshBluetoothService.ACTION_GATT_RECONNECTING);
        mBroadcastManager.registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mBroadcastManager.unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStackService.getMeshBluetoothService().disconnect();
    }

    /*@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == BluetoothHelper.BLUETOOTH_REQUEST) {
            if (resultCode == RESULT_CANCELED) finish();
        }
    }*/

}
