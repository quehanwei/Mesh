package ru.ximen.mesh;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import ru.ximen.mesh.DeviceFragment.OnListFragmentInteractionListener;
import ru.ximen.mesh.dummy.DummyContent.DummyItem;
import ru.ximen.meshstack.MeshDevice;
import ru.ximen.meshstack.MeshNetwork;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyDeviceRecyclerViewAdapter extends RecyclerView.Adapter<MyDeviceRecyclerViewAdapter.ViewHolder> {

    private final List<MeshDevice> mDevices;
    private final OnListFragmentInteractionListener mListener;

    public MyDeviceRecyclerViewAdapter(MeshNetwork network, OnListFragmentInteractionListener listener) {
        mDevices = network.getDevices();
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DeviceView dView = new DeviceView(parent.getContext());
        dView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        //View view = LayoutInflater.from(parent.getContext())
        //        .inflate(R.layout.fragment_device, parent, false);
        return new ViewHolder(dView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        MeshDevice device = mDevices.get(position);
        holder.mItem = device;
        //holder.mNameView.setText(device.getName());
        //holder.mAddrView.setText(String.valueOf(device.getAddress()));

        holder.getCustomView().setDevice(device);

        holder.mView.setOnClickListener(v -> {
            if (null != mListener) {
                // Notify the active callbacks interface (the activity, if the
                // fragment is attached to one) that an item has been selected.
                mListener.onListFragmentInteraction(holder.mItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDevices.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final DeviceView mView;
        //public final TextView mNameView;
        //public final TextView mAddrView;
        //public final Switch mSwitchView;
        public MeshDevice mItem;

        public ViewHolder(View view) {
            super(view);
            mView = (DeviceView) view;
        }

        public DeviceView getCustomView() {
            return mView;
        }
        @Override
        public String toString() {
            return super.toString() + " '" + mItem.getName() + "'";
        }
    }

    MeshDevice getItemAt(int position){
        return  mDevices.get(position);
    }
}
