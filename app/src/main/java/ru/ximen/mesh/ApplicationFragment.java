package ru.ximen.mesh;

import android.app.Service;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import ru.ximen.mesh.dummy.DummyContent;
import ru.ximen.mesh.dummy.DummyContent.DummyItem;
import ru.ximen.meshstack.MeshStackService;

import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class ApplicationFragment extends Fragment {
    // TODO: Customize parameter argument names
    //private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    //private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private MeshStackService mStackService;
    boolean isBound = false;
    private RecyclerView recyclerView;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ApplicationFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static ApplicationFragment newInstance() {
        ApplicationFragment fragment = new ApplicationFragment();
        /*Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);*/
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            //mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }

        Intent service = new Intent(getActivity(), MeshStackService.class);
        getActivity().bindService(service, mServiceConnection, Service.BIND_AUTO_CREATE);

        SharedViewModel sharedViewModel = ViewModelProviders.of(getActivity()).get(SharedViewModel.class);
        sharedViewModel.creatingApplication().observe(this, name -> {
            mStackService.getAppManager().createAplication(name);
            mStackService.getNetworkManager().updateNetwork();
            recyclerView.setAdapter(new MyApplicationRecyclerViewAdapter(mStackService.getAppManager(), mListener));
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_application_list, container, false);
        Log.d("Mesh", "onCreateView!!");
        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            if(isBound) {
                recyclerView.setAdapter(new MyApplicationRecyclerViewAdapter(mStackService.getAppManager(), mListener));
            }
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("Mesh", "onAttach!!");
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
        void onListFragmentInteraction(String item);
    }

    public ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("Mesh", "Service connected!");
            isBound = true;
            MeshStackService.LocalBinder binder = (MeshStackService.LocalBinder) service;
            mStackService = binder.getService();
            Log.d("Mesh", mStackService.getNetworkManager().getCurrentNetwork().getName());
            recyclerView.setAdapter(new MyApplicationRecyclerViewAdapter(mStackService.getAppManager(), mListener));
        }

        public void onServiceDisconnected(ComponentName className) {
            mStackService = null;
            Log.d("Mesh", "Service disconnected!");
        }

    };

    public void createApplication(String name){
        mStackService.getAppManager().createAplication(name);
        recyclerView.setAdapter(new MyApplicationRecyclerViewAdapter(mStackService.getAppManager(), mListener));

    }
}
