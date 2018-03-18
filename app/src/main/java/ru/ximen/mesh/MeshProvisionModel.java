package ru.ximen.mesh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import au.com.ds.ef.EasyFlow;
import au.com.ds.ef.Event;
import au.com.ds.ef.FlowBuilder;
import au.com.ds.ef.State;
import au.com.ds.ef.StatefulContext;
import au.com.ds.ef.call.StateHandler;

/**
 * Created by ximen on 18.03.18.
 */

public class MeshProvisionModel {
    private static class FlowContext extends StatefulContext {
    }
    // defining states
    private final State<FlowContext> INVITATION = FlowBuilder.state();
    private final State<FlowContext> CANCEL = FlowBuilder.state();
    // defining events
    private final Event<FlowContext> onStart = FlowBuilder.event();
    private final Event<FlowContext> onInviteTimeout = FlowBuilder.event();


    private EasyFlow<FlowContext> flow;

    private final Context mContext;
    final static private String TAG = "MeshProvision";

    public MeshProvisionModel(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter(MeshService.ACTION_PROVISION_DATA_AVAILABLE);
        mContext.registerReceiver(mGattUpdateReceiver, filter);
        initFlow();
        bindFlow();
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "Got provision data: ");
        }
    };

    public void startProvision(){
        flow.start(new FlowContext());
    }

    public void close() {
        mContext.unregisterReceiver(mGattUpdateReceiver);
    }

    private void initFlow() {
        if (flow != null) {
            return;
        }
        flow = FlowBuilder
                .from(INVITATION).transit(
                        onInviteTimeout.to(CANCEL).transit(
                                onStart.to(INVITATION)
                        )
                ).executor(new UiThreadExecutor());
    }

    private void bindFlow() {
        INVITATION.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, FlowContext context) throws Exception {
                // send INVITE PDU
                // start timer
            }
        });
        CANCEL.whenEnter(new StateHandler<FlowContext>() {
            @Override
            public void call(State<FlowContext> state, FlowContext context) throws Exception {
                // tide up to initial state
            }
        });
    }
}
