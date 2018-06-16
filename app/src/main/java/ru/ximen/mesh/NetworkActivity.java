package ru.ximen.mesh;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class NetworkActivity extends AppCompatActivity {
    private final String TAG = "NetworkActivity";
    private DeviceListAdapter mLstAdapter;
    private MeshNetwork mNetwork;

    // TODO: Connect to selected network through best RSSI proxy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNetwork = ((MeshApplication) getApplicationContext()).getManager().selectNetowrk(getIntent().getStringExtra("ru.ximen.mesh.NETWORK"));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NetworkActivity.this, ScanActivity.class);
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
                final MeshDevice item = (MeshDevice) parent.getItemAtPosition(position);
                Log.d(TAG, item.getName());
                Toast.makeText(getApplicationContext(), "Connecting device " + item.getName(), Toast.LENGTH_LONG).show();
                ((MeshApplication) getApplicationContext()).getMeshService().connect(item);
            }

        });

        // TODO: Menu to delete network
    }

    @Override
    protected void onResume() {
        mLstAdapter.notifyDataSetChanged();
        super.onResume();
    }
}
