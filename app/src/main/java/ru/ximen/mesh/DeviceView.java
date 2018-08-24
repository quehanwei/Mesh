package ru.ximen.mesh;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import ru.ximen.meshstack.MeshDevice;

public class DeviceView extends LinearLayout {
    private MeshDevice mDevice;
    private Switch mSwitchView;
    private TextView mNameView;
    private TextView mAddrView;

    public DeviceView(Context context) {
        this(context, null);
    }

    public DeviceView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeviceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DeviceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        inflate(getContext(), R.layout.fragment_device, this);
    }

    public void setDevice(MeshDevice device){
        mDevice = device;
        mSwitchView = findViewById(R.id.onoff_switch);
        mNameView = findViewById(R.id.device_name);
        mAddrView = findViewById(R.id.device_address);

        mNameView.setText(device.getName());
        mAddrView.setText(String.valueOf(device.getAddress()));

    }
}
