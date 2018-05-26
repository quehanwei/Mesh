package ru.ximen.mesh;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class StartActivity extends AppCompatActivity {
    //private MeshManager manager;
    ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ArrayList<String> filesList = new ArrayList<>();
        filesList.addAll(((MeshApplication) getApplication()).getManager().listNetworks());
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filesList);
        ListView lv = findViewById(R.id.listView);
        lv.setAdapter(listAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = (String) parent.getItemAtPosition(position);
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                intent.putExtra("NETWORK", item);
                startActivity(intent);
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
                        MeshNetwork network = ((MeshApplication) getApplication()).getManager().createNetwork(input.getText().toString());
                        Intent intent = new Intent(StartActivity.this, MainActivity.class);
                        intent.putExtra("ru.ximen.mesh.NETWORK", network.toJSON().toString());
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

}
