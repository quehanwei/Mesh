package ru.ximen.mesh;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import ru.ximen.meshstack.MeshStackService;

public class BasicServiceActivty extends AppCompatActivity {
    protected MeshStackService mStackService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic);
        attachService();
    }

    @Override
    protected void onDestroy() {
        detachService();
        super.onDestroy();
    }

    public ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            MeshStackService.LocalBinder binder = (MeshStackService.LocalBinder) service;
            mStackService = binder.getService();
            onServiceAttached(mStackService);
        }

        public void onServiceDisconnected(ComponentName className) {
            mStackService = null;
        }

    };

    private void attachService() {
        Intent service = new Intent(this, MeshStackService.class);
        bindService(service, mServiceConnection, Service.BIND_AUTO_CREATE);
    }

    private void detachService() {
        unbindService(mServiceConnection);
    }

    /** Callback when service attached. */
    protected void onServiceAttached(MeshStackService service) {
        // do something necessary by its subclass.
    }
}
