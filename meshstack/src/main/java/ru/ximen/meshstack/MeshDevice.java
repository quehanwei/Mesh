package ru.ximen.meshstack;

import android.bluetooth.BluetoothDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by ximen on 12.05.18.
 */

public class MeshDevice {
    private short mAddress;
    private String mMAC;
    private String mName;
    private byte[] mDeviceKey;
    //private UUID mUUID;
    private boolean isProxy = true;
    private boolean isRelay = true;
    private boolean isLowPower = false;
    private boolean isFriend = false;
    private ArrayList<MeshElement> elements;

    public MeshDevice(JSONObject json) {
        elements = new ArrayList<>();
        try {
            mAddress = (short) json.getInt("address");
            mMAC = json.getString("mac");
            mName = json.getString("name");
            mDeviceKey = Utils.hexString2Bytes(json.getString("deviceKey"));
            JSONArray features = json.getJSONArray("features");
            for (int i = 0; i < features.length(); i++) {
                isProxy = features.getString(i).equals("proxy");
                isRelay = features.getString(i).equals("relay");
                isFriend = features.getString(i).equals("friend");
                isLowPower = features.getString(i).equals("low");
            }

            JSONArray elem = json.getJSONArray("elements");
            for (int i = 0; i < elem.length(); i++) {
                JSONObject obj = elem.getJSONObject(i);
                MeshElement newElement = new MeshElement(obj.getString("name"), (short) obj.getInt("address"));
                JSONArray models = obj.getJSONArray("models");
                for (int j = 0; j < models.length(); j++) {
                    JSONObject modelObj = models.getJSONObject(j);
                    newElement.addModel(newElement.newModel((byte) modelObj.getInt("aid"), Utils.hexString2Bytes(modelObj.getString("id"))));
                }
                elements.add(newElement);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public MeshDevice(BluetoothDevice device, short address, byte[] deviceKey) {
        elements = new ArrayList<>();
        mAddress = address;
        mMAC = device.getAddress();
        mDeviceKey = deviceKey;

    }

    public short getAddress() {
        return mAddress;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("address", Integer.valueOf(mAddress));
            json.put("mac", mMAC);
            json.put("deviceKey", Utils.toHexString(mDeviceKey));
            json.put("name", mName);
            JSONArray features = new JSONArray();
            if (isProxy()) features.put("proxy");
            if (isRelay()) features.put("relay");
            if (isFriend()) features.put("friend");
            if (isLowPower()) features.put("low");

            json.put("features", features);
            JSONArray elem = new JSONArray();
            for ( MeshElement element : elements) {
                JSONObject elemObj = new JSONObject();
                elemObj.put("name", element.getName());
                elemObj.put("address", element.getAddress());
                JSONArray modArray = new JSONArray();
                for (MeshElement.Model model: element.getModels()) {
                    JSONObject modObj = new JSONObject();
                    modObj.put("aid", model.getAid());
                    modObj.put("id", model.getId());
                    modArray.put(modObj);
                }
                elemObj.put("models", modArray);
                elem.put(elemObj);
            }
            json.put("elements", elem);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getName() {
        return mName;
    }

    public String getMAC() {
        return mMAC;
    }

    public byte[] getDeviceKey() {
        return mDeviceKey;
    }

    public boolean isProxy() {
        return isProxy;    // TODO: Replace to configuration result
    }

    public boolean isRelay() {
        return isRelay;    // TODO: Replace to configuration result
    }

    public boolean isFriend() {
        return isFriend;    // TODO: Replace to configuration result
    }
    public boolean isLowPower() {
        return isLowPower;    // TODO: Replace to configuration result
    }

    public void setConfiguration(MeshCompositionData data){
        elements = data.getElements();
        isRelay = data.isRelay();
        isProxy = data.isProxy();
        isFriend = data.isFriend();
        isLowPower = data.isLowPower();
    }

    public ArrayList<MeshElement> getElements() {
        return elements;
    }
}
