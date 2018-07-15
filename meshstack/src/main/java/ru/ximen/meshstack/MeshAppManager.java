package ru.ximen.meshstack;

import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

public class MeshAppManager {
    private HashMap<String, Pair> keys;

    public MeshAppManager() {
        keys = new HashMap<>();
    }

    public void createAplication(String name){
        byte[] AppKey = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(AppKey);
        byte AID = MeshEC.k4(AppKey);
        Pair<Byte, byte[]> pair = new Pair<>(AID, AppKey);
        keys.put(name, pair);
    }

    public void fromJSON(JSONArray json){
        keys.clear();
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject app = json.getJSONObject(i);
                String name = app.getString("name");
                byte[] key = Utils.hexString2Bytes(app.getString("key"));
                byte AID = MeshEC.k4(key);
                keys.put(name, new Pair(AID, key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    
    public JSONArray toJSON(){
        JSONArray json = new JSONArray();
        for (Map.Entry<String, Pair> entry : keys.entrySet()) {
            JSONObject app = new JSONObject();
            try {
                app.put("name", entry.getKey());
                app.put("key", Utils.toHexString((byte[])entry.getValue().second));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            json.put(app);
        }
        return json;
    }
}
