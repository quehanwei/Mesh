package ru.ximen.mesh;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import ru.ximen.meshstack.MeshApplication;
import ru.ximen.meshstack.MeshCompositionDataProc;
import ru.ximen.meshstack.MeshConfigurationClient;
import ru.ximen.meshstack.MeshDevice;
import ru.ximen.meshstack.MeshModel;
import ru.ximen.meshstack.MeshNetwork;
import ru.ximen.meshstack.MeshProcedure;
import ru.ximen.meshstack.MeshStatusResult;
import ru.ximen.meshstack.*;

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
                                MeshConfigurationClient conf = new MeshConfigurationClient((MeshApplication) (rowView.getContext().getApplicationContext()), getItem(position).getAddress());
                                ((MeshCompositionDataProc) (conf.getModel(MeshModel.ID_CONFIGURATION_MODEL_CLIENT).procedure("CompositionData"))).setStatusListner(new MeshProcedure.MeshMessageCallback() {
                                    @Override
                                    public void status(MeshStatusResult result) {

                                        MeshConfigurationDialog dlg = new MeshConfigurationDialog();
                                        Bundle bundle = new Bundle();
                                        bundle.putByteArray("composition", result.getData());
                                        dlg.setArguments(bundle);
                                        dlg.show(((NetworkActivity)mContext).getSupportFragmentManager(), "Configuration");
                                    }
                                });
                                ((MeshCompositionDataProc) (conf.getModel(MeshModel.ID_CONFIGURATION_MODEL_CLIENT).procedure("CompositionData"))).get((byte) 0);
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
