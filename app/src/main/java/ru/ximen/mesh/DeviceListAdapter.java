package ru.ximen.mesh;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends BaseAdapter {
    final static private String TAG = "MeshAdapter";
    private final Context mContext;
    private final MeshNetwork mNetwork;
    private final LayoutInflater mInflater;

    public DeviceListAdapter(Context context, MeshNetwork network) {
        //super(context, resource, textViewResourceId);
        mContext = context;
        mNetwork = network;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mNetwork.getDevices().size();
    }

    @Override
    public MeshDevice getItem(int position) {
        return mNetwork.getDevices().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View rowView;
        if (convertView == null) {
            rowView = mInflater.inflate(R.layout.list_item_device, parent, false);

        } else {
            rowView = convertView;
        }
        TextView textView = rowView.findViewById(R.id.textName);
        textView.setText(getItem(position).getName());
        ImageView imageView = rowView.findViewById(R.id.menu_imageview);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.device_list_popup, popup.getMenu());
                popup.show();
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.configure:
                                //Snackbar.make(rowView, "Not implemented yet", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                                MeshConfigurationClient conf = new MeshConfigurationClient((MeshApplication) (rowView.getContext().getApplicationContext()), getItem(position).getAddress());
                                //MeshOnOffClient conf = new MeshOnOffClient((MeshApplication)(rowView.getContext().getApplicationContext()), getItem(position).getAddress());
                                ((MeshGATTProxyProc) (conf.getModel(MeshModel.ID_CONFIGURATION_MODEL_CLIENT).procedure("GATTProxy"))).setStatusListner(new MeshProcedure.MeshMessageCallback() {
                                    @Override
                                    public void status(MeshStatusResult result) {
                                        Log.d(TAG, "Got result: " + Utils.toHexString(result.getData()));
                                        Snackbar.make(rowView, "GATT Proxy status: " + Utils.toHexString(result.getData()), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                                    }
                                });
                                ((MeshGATTProxyProc) (conf.getModel(MeshModel.ID_CONFIGURATION_MODEL_CLIENT).procedure("GATTProxy"))).get();
                                break;
                            case R.id.delete:
                                mNetwork.deleteDevice(getItem(position));
                                notifyDataSetChanged();
                                break;
                            case R.id.unprovision:
                                Snackbar.make(rowView, "Not implemented yet", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });
            }
        });
        return rowView;
    }
}
