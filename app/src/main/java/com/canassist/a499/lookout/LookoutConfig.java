package com.canassist.a499.lookout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class LookoutConfig {
    private BluetoothConnection mBluetoothConnection;
    private boolean mIsLookoutActive;
    private String mMessage;
    private ArrayList<String> mPhoneNumbers;
    private String mStartTime;
    private String mEndTime;
    private String mInactivityTimeOut;

    private JSONObject jsonObject;
    private boolean mShouldUpdateUI = false;
    private boolean mShouldSetDefaults = false;

    public LookoutConfig() {
        mPhoneNumbers = new ArrayList<>();
    }

    public void setBluetoothConnectionObject(BluetoothConnection bluetoothConnection) {
        mBluetoothConnection = bluetoothConnection;
    }

    public void receivedSettings(String settings) {
        try {
            if (settings.equalsIgnoreCase("reset defaults")) {
                mShouldSetDefaults = true;
            } else {
                jsonObject = new JSONObject(settings);
                mIsLookoutActive = jsonObject.getBoolean("isActive");
                mMessage = jsonObject.getString("message");
                mPhoneNumbers.clear();
                JSONArray array = jsonObject.getJSONArray("numbers");
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        mPhoneNumbers.add(array.getString(i));
                    }
                }
                mStartTime = jsonObject.getString("startTime");
                mEndTime = jsonObject.getString("endTime");
                mInactivityTimeOut = jsonObject.getString("inactivityTimeout");
                mShouldUpdateUI = true;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getJSONObject(){
        return jsonObject;
    }

    public boolean shouldUIUpdate() {
        return mShouldUpdateUI;
    }

    public void setShouldUpdateUI(boolean b) {
        mShouldUpdateUI = b;
    }

    public boolean shouldResetDefaults() {
        return mShouldSetDefaults;
    }

    public void setShouldResetDefaults(boolean b) {
        mShouldSetDefaults = b;
    }

    public void setIsLookoutActive(boolean b, boolean sendToLookout) {
        mIsLookoutActive = b;
        if (sendToLookout) {
            sendToLookout();
        }
    }

    public void addNumber(String number, boolean sendToLookout) {
        mPhoneNumbers.add(number);
        if (sendToLookout) {
            sendToLookout();
        }
    }

    public void clearNumbers(boolean sendToLookout) {
        mPhoneNumbers.clear();
        if (sendToLookout) {
            sendToLookout();
        }
    }

    public void removeNumber(int i, boolean sendToLookout) {
        if (i < mPhoneNumbers.size()) {
            mPhoneNumbers.remove(i);
            if (sendToLookout) {
                sendToLookout();
            }
        }
    }

    public void setMessage(String message, boolean sendToLookout) {
        mMessage = message;
        if (sendToLookout) {
            sendToLookout();
        }
    }

    public void setStartTime(int hour, int minute, boolean sendToLookout) {
        mStartTime = String.format("%02d:", hour) + String.format("%02d", minute);
        if (sendToLookout) {
            sendToLookout();
        }
    }

    public void setEndTime(int hour, int minute, boolean sendToLookout) {
        mEndTime = String.format("%02d:", hour) + String.format("%02d", minute);
        if (sendToLookout) {
            sendToLookout();
        }
    }

    public void setInactivityTimeOut(int hour, int minute, boolean isDemo, boolean sendToLookout) {
        if (isDemo) {
            mInactivityTimeOut = "00:00:10";
        }
        else {
            mInactivityTimeOut = String.format("%02d:", hour) + String.format("%02d:", minute) + String.format("%02d", 0);
        }
        if (sendToLookout) {
            sendToLookout();
        }
    }

    public void sendToLookout() {
        if (mBluetoothConnection != null) {
            jsonObject = new JSONObject();
            try {
                jsonObject.put("isActive", mIsLookoutActive);
                jsonObject.put("message", mMessage);
                jsonObject.put("numbers", new JSONArray(mPhoneNumbers));
                jsonObject.put("startTime", mStartTime);
                jsonObject.put("endTime", mEndTime);
                jsonObject.put("inactivityTimeout", mInactivityTimeOut);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mBluetoothConnection.sendData(jsonObject.toString().getBytes());
        }
    }

}
