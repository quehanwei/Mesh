package ru.ximen.mesh;

import android.app.Service;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import ru.ximen.mesh.dummy.DummyContent;
import ru.ximen.mesh.dummy.DummyContent.DummyItem;
import ru.ximen.meshstack.MeshDevice;
import ru.ximen.meshstack.MeshNetwork;
import ru.ximen.meshstack.MeshStackService;

import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class DeviceFragment extends Fragment {
    private static final int REQUEST_ENABLE_BT = 1;
    private final String TAG = DeviceFragment.class.getSimpleName();
    private static final String ARG_NETWORK = "network";
    private String mNetworkName;
    private MeshStackService mStackService;
    private MeshNetwork mNetwork;
    boolean isBound = false;
    private MyDeviceRecyclerViewAdapter mLstAdapter;
    private OnListFragmentInteractionListener mListener;
    private RecyclerView recyclerView;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public DeviceFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static DeviceFragment newInstance(String networkName) {
        DeviceFragment fragment = new DeviceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NETWORK, networkName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent service = new Intent(getActivity(), MeshStackService.class);
        getActivity().bindService(service, mServiceConnection, Service.BIND_AUTO_CREATE);

        if (getArguments() != null) {
            mNetworkName = getArguments().getString(ARG_NETWORK);
        }

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            if(isBound) {
                mLstAdapter = new MyDeviceRecyclerViewAdapter(mNetwork, mListener);
                recyclerView.setAdapter(mLstAdapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            }
        }

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener((recyclerView, view1, position) -> {
        });
        ItemClickSupport.addTo(recyclerView).setOnItemLongClickListener((recyclerView, view12, position) -> {
            PopupMenu popup = new PopupMenu(view12.getContext(), view12);
            popup.getMenuInflater().inflate(R.menu.device_list_popup, popup.getMenu());
            popup.show();
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.configure:
                        break;
                    case R.id.delete:
                        new AlertDialog.Builder(getActivity())
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("Delete confirmation")
                                .setMessage("Are you sure you want to delete this device?")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    mNetwork.deleteDevice(mLstAdapter.getItemAt(position));
                                    recyclerView.setAdapter(mLstAdapter);
                                })
                                .setNegativeButton("No", null)
                                .show();
                        break;
                    case R.id.unprovision:
                        break;
                    default:
                        break;
                }
                return true;
            });
            return true;
        });
        return view;
    }


    @Override
    public void onResume() {
        if(isBound) {
            mLstAdapter.notifyDataSetChanged();
            if (!mStackService.getMeshBluetoothService().isConnected()) {
                Toast.makeText(getActivity(), "Connecting to network " + mNetwork.getName(), Toast.LENGTH_SHORT).show();
                if (mNetwork.getDevices().size() > 0)
                    mStackService.getMeshBluetoothService().connect(findProxy());
            }
        }
        super.onResume();
    }

    private MeshDevice findProxy() {
        for (MeshDevice device : mNetwork.getDevices()) {
            if (device.isProxy()) return device;
            //BluetoothDevice bDevice = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter().getRemoteDevice(device.getMAC());
        }
        return null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        getActivity().unbindService(mServiceConnection);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(MeshDevice item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if (resultCode != RESULT_OK) getActivity().finish();
        }
    }

    public ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            isBound = true;
            MeshStackService.LocalBinder binder = (MeshStackService.LocalBinder) service;
            mStackService = binder.getService();
            mNetwork = mStackService.getNetworkManager().selectNetwork(mNetworkName);
            mLstAdapter = new MyDeviceRecyclerViewAdapter(mNetwork, mListener);
            recyclerView.setAdapter(mLstAdapter);
            if (!mStackService.getMeshBluetoothService().isConnected()) {
                Toast.makeText(getActivity(), "Connecting to network " + mNetwork.getName(), Toast.LENGTH_SHORT).show();
                if (mNetwork.getDevices().size() > 0)
                    mStackService.getMeshBluetoothService().connect(findProxy());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mStackService = null;
        }

    };

}
