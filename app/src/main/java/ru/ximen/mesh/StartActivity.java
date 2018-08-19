package ru.ximen.mesh;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import ru.ximen.meshstack.MeshNetwork;
import ru.ximen.meshstack.MeshStackService;

public class StartActivity extends BasicServiceActivty {
    public static final int IDM_DELETE = 1001;

    ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If network was previously selected and set autoload, simply start it
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        if (sharedPref.getBoolean("autoload", false)){
            String network = sharedPref.getString("network", "");
            Intent intent = new Intent(StartActivity.this, NetworkActivity.class);
            intent.putExtra("ru.ximen.mesh.NETWORK", network);
            Log.d("StartActivity", "Network name: " + network);
            startActivity(intent);
        }

        setContentView(R.layout.activity_start);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startService(new Intent(this, MeshStackService.class));
    }

    @Override
    protected void onServiceAttached(MeshStackService service) {
        super.onServiceAttached(service);
        ArrayList<String> filesList = new ArrayList<>();
        filesList.addAll(mStackService.getNetworkManager().listNetworks());
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filesList);
        ListView lv = findViewById(R.id.listView);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                Intent intent = new Intent(StartActivity.this, NetworkActivity.class);
                intent.putExtra("ru.ximen.mesh.NETWORK", item);
                Log.d("StartActivity", "Network name: " + item);
                CheckBox cb = findViewById(R.id.checkBox);
                if(cb.isChecked()){
                    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("autoload", true);
                    editor.putString("network", item);
                    editor.apply();
                }
                startActivity(intent);
            }
        });
        lv.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                menu.add(Menu.NONE, IDM_DELETE, Menu.NONE, "Delete");
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
                builder.setTitle("New mesh network");

                final EditText input = new EditText(StartActivity.this);

                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MeshNetwork network = mStackService.getNetworkManager().createNetwork(input.getText().toString());
                        Intent intent = new Intent(StartActivity.this, NetworkActivity.class);
                        intent.putExtra("ru.ximen.mesh.NETWORK", input.getText().toString());
                        Log.d("StartActivity", "Network name: " + input.getText().toString());
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case IDM_DELETE:
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
                final ListView lv = findViewById(R.id.listView);
                String selectedItem = (String) lv.getItemAtPosition(info.position);
                mStackService.getNetworkManager().deleteNetwork(selectedItem);
                Toast toast = Toast.makeText(StartActivity.this, "Deleting network " + selectedItem, Toast.LENGTH_SHORT);
                toast.show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayList<String> filesList = new ArrayList<>();
                        filesList.addAll(mStackService.getNetworkManager().listNetworks());
                        listAdapter = new ArrayAdapter<String>(StartActivity.this, android.R.layout.simple_list_item_1, filesList);
                        lv.setAdapter(listAdapter);
                    }
                });
                break;
            default:
                //return super.onContextItemSelected(item);
        }
        return true;
    }

}
