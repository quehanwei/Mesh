package ru.ximen.mesh;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class NetworkActivity extends AppCompatActivity {

    private BluetoothMesh mesh;
    private DeviceListAdapter mLstAdapter;
    private MeshNetwork mNetwork;

    // TODO: Connect to selected network through best RSSI proxy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNetwork = ((MeshApplication) getApplicationContext()).getManager().getNetwork(getIntent().getStringExtra("ru.ximen.mesh.NETWORK"));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NetworkActivity.this, MainActivity.class);
                intent.putExtra("ru.ximen.mesh.NETWORK", mNetwork.getName());
                startActivity(intent);
            }
        });

        final ListView listview = findViewById(R.id.listView);

        mLstAdapter = new DeviceListAdapter(this, mNetwork);
        listview.setAdapter(mLstAdapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                //final String item = (String) parent.getItemAtPosition(position);
                //Toast.makeText(getApplicationContext(), "Connecting device", Toast.LENGTH_LONG).show();
                //mesh.connect(mDeviceMap.get(item));
            }

        });

        //mesh = BluetoothMesh.getInstance(getApplicationContext(), getIntent().getStringExtra("ru.ximen.mesh.NETWORK"));
        // TODO: Menu to delete network
    }

    @Override
    protected void onResume() {
        mLstAdapter.notifyDataSetChanged();
        super.onResume();
    }
}
