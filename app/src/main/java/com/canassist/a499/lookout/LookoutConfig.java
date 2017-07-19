package com.canassist.a499.lookout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by lynx on 16/07/17.
 */

public class LookoutConfig {
    private BluetoothConnection mBluetoothConnection;
    private ArrayList<String> mPhoneNumbers;
    private JSONObject jsonObject;
    private String jsonString;

    public LookoutConfig() {
        mPhoneNumbers = new ArrayList<>();
    }

    public void setBluetoothConnectionObject(BluetoothConnection bluetoothConnection) {
        mBluetoothConnection = bluetoothConnection;
    }

    public void addNumber(String number) {
        mPhoneNumbers.add(number);
        sendToLookout();
    }

    public void removeNumber(int i) {
        if (i < mPhoneNumbers.size()) {
            mPhoneNumbers.remove(i);
            sendToLookout();
        }
    }

    public void sendToLookout() {
        if (mBluetoothConnection != null) {
            jsonObject = new JSONObject();
            try {
                jsonObject.put("numbers", new JSONArray(mPhoneNumbers));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mBluetoothConnection.sendData(jsonObject.toString().getBytes());
        }
    }

}
