package ru.ximen.mesh;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import ru.ximen.meshstack.MeshDevice;
import ru.ximen.meshstack.MeshNetwork;
import ru.ximen.meshstack.MeshStackService;


public class NetworkActivity extends BasicServiceActivty {
    private final String TAG = "NetworkActivity";
    private DeviceListAdapter mLstAdapter;
    private MeshNetwork mNetwork;
    boolean isBound = false;

    // TODO: Connect to selected network through best RSSI proxy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        startService(new Intent(this, MeshStackService.class));
    }

    @Override
    protected void onServiceAttached(MeshStackService service) {
        super.onServiceAttached(service);
        mNetwork = mStackService.getNetworkManager().selectNetwork(getIntent().getStringExtra("ru.ximen.mesh.NETWORK"));
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NetworkActivity.this, ScanActivity.class);
                intent.putExtra("ru.ximen.mesh.NETWORK", mNetwork.getName());
                startActivity(intent);

//                MeshNetworkPDU pdu = new MeshNetworkPDU(mNetwork, (short) 0xfffd, (byte) 1, (byte) 0);
//                pdu.setData(Utils.hexString2Bytes("034b50057e400000010000"));
//                byte[] data = pdu.data();
//                Log.d("Mesh", Utils.toHexString(data));
//                MeshNetworkPDU pdu2 = new MeshNetworkPDU(mNetwork, data);
            }
        });

        final ListView listview = findViewById(R.id.listView);

        mLstAdapter = new DeviceListAdapter(this, mStackService);
        listview.setAdapter(mLstAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final MeshDevice item = (MeshDevice) parent.getItemAtPosition(position);
                Log.d(TAG, item.getName());
                Toast.makeText(getApplicationContext(), "Connecting device " + item.getName(), Toast.LENGTH_LONG).show();
                mStackService.getMeshBluetoothService().connect(item);
            }

        });
        isBound = true;

        // TODO: Menu to delete network
    }

    @Override
    protected void onResume() {
        if(isBound) {
            mLstAdapter.notifyDataSetChanged();
        }
        super.onResume();
    }

    @Override
    protected void onStart() {
        /*if (!mStackService.getMeshBluetoothService().isConnected()) {
            Toast.makeText(this, "Connecting to network " + mNetwork.getName(), Toast.LENGTH_SHORT).show();
            if (mNetwork.getDevices().size() > 0)
                mStackService.getMeshBluetoothService().connect(findProxy());
        }*/
        super.onStart();
    }

    @Override
    protected void onStop() {
        //((MeshApplication) getApplicationContext()).getMeshService().disconnect();
        super.onStop();
    }

    private MeshDevice findProxy() {
        for (MeshDevice device : mNetwork.getDevices()) {
            if (device.isProxy()) return device;
            //BluetoothDevice bDevice = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getRemoteDevice(device.getMAC());
        }
        return null;
    }
}
