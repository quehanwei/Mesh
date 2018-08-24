package ru.ximen.mesh;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import ru.ximen.meshstack.MeshDevice;
import ru.ximen.meshstack.MeshElement;
import ru.ximen.meshstack.MeshModel;

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

        ArrayList<MeshElement> elements = device.getElements();
        if(elements.size() > 0) {
            if (!elements.get(0).getModels().contains(MeshModel.ID_ONOFF_MODEL_SERVER)) {
                findViewById(R.id.onoff_switch).setVisibility(INVISIBLE);
            }
            if (!elements.get(0).getModels().contains(MeshModel.ID_LEVEL_MODEL_SERVER)) {
                findViewById(R.id.seekBar).setVisibility(INVISIBLE);
            }
        } else {
            findViewById(R.id.onoff_switch).setVisibility(INVISIBLE);
            findViewById(R.id.seekBar).setVisibility(INVISIBLE);
        }
        if(elements.size() > 1) {
            LayoutParams lparams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            LinearLayout linearLayout = findViewById(R.id.device_layout);
            for ( MeshElement element : elements ) {
                View v = inflater.inflate(R.layout.element, null);
                TextView tv_name = findViewById(R.id.elem_name);
                tv_name.setText(element.getName());
                TextView tv_addr = findViewById(R.id.elem_address);
                tv_addr.setText(element.getAddress());
                if(!element.getModels().contains(MeshModel.ID_ONOFF_MODEL_SERVER)){
                    findViewById(R.id.elem_switch).setVisibility(INVISIBLE);
                }
                if(!element.getModels().contains(MeshModel.ID_LEVEL_MODEL_SERVER)){
                    findViewById(R.id.elem_seekBar).setVisibility(INVISIBLE);
                }
                linearLayout.addView(v);
            }
        }
    }
}
