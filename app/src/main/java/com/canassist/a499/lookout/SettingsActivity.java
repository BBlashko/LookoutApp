package com.canassist.a499.lookout;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.UUID;

public class SettingsActivity extends AppCompatActivity {

    //Config
    private static final String TAG = "[SettingsActivity]";

    //Variables
    private Context mContext;
    private LookoutConfig mLookoutConfig;

    //Connection Information
    private BluetoothDevice mLookoutDevice;
    private static final UUID mLookoutUUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    private BluetoothConnection mBluetoothConnection;
    private CheckConnectionThread mCheckConnectionThread;

    //UI
    private ProgressDialog mProgressDialog;
    private ToggleButton mDisableEnableButton;
    private ImageButton mAddNumber;
    private ContactListAdapter mNumberListAdapter;
    private ListView mNumberListView;
    private EditText mMessageEditText;
    private Spinner mTimeoutHoursSpinner;
    private Spinner mTimeoutMinutesSpinner;
    private ToggleButton mTimeoutDemoToggleBtn;
    private Button mStartTimeBtn;
    private Button mEndTimeBtn;
    private TextView mStartTimeTV;
    private TextView mEndTimeTV;
    private Button mResetBtn;

    private boolean mInitComplete = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        init();
        mInitComplete = true;
    }

    @Override
    public void onBackPressed() {
        sendToast("Closing Bluetooth Connection...");
        finishActivity();
    }

    private void finishActivity(){
        if (mBluetoothConnection != null && mBluetoothConnection.getConnectedCommunicationThread() != null) {
            mBluetoothConnection.getConnectedCommunicationThread().cancel();
        }
        if (mBluetoothConnection != null && mBluetoothConnection.getConnectionThread() != null) {
            mBluetoothConnection.getConnectionThread().cancel();
        }
        finish();
    }

    @Override
    protected void onPause(){
        super.onPause();
        finishActivity();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "Destroying SettingsActivity");
        if (mBluetoothConnection != null && mBluetoothConnection.getConnectedCommunicationThread() != null) {
            mBluetoothConnection.getConnectedCommunicationThread().cancel();
        }
        if (mBluetoothConnection != null && mBluetoothConnection.getConnectionThread() != null) {
            mBluetoothConnection.getConnectionThread().cancel();
        }

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void init() {
        mContext = SettingsActivity.this;
        mLookoutDevice = getIntent().getParcelableExtra("LOOKOUT_DEVICE");

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        TextView tv = (TextView) findViewById(R.id.set_tv_dev_name);
        tv.setText(mLookoutDevice.getName());

        mDisableEnableButton = (ToggleButton) findViewById(R.id.sett_toggle_on_off_btn);
        mDisableEnableButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                mLookoutConfig.setIsLookoutActive(isChecked, true);
            }
        });

        mLookoutConfig = new LookoutConfig();
        ArrayList<String> list = new ArrayList<String>();
        mNumberListAdapter = new ContactListAdapter(list, this, mLookoutConfig);

        //handle listview and assign adapter
        mNumberListView = (ListView)findViewById(R.id.sett_contact_list);
        TextView empty = (TextView) findViewById(R.id.empty);
        mNumberListView.setEmptyView(empty);
        mNumberListView.setAdapter(mNumberListAdapter);

        mAddNumber = (ImageButton) findViewById(R.id.add_number_btn);
        mAddNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNumberListAdapter.createNewNumberField();
                mNumberListView.setSelection(mNumberListAdapter.getCount() - 1);
            }
        });

        //message
        mMessageEditText = (EditText) findViewById(R.id.sett_message_input);
        mMessageEditText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                if (mInitComplete) {
                    mLookoutConfig.setMessage(s.toString(), true);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        //Start and End time
        mStartTimeBtn = (Button) findViewById(R.id.sett_btn_edit_start_time);
        mEndTimeBtn = (Button) findViewById(R.id.sett_btn_edit_end_time);
        mStartTimeTV = (TextView) findViewById(R.id.sett_tv_start_time);
        mEndTimeTV = (TextView) findViewById(R.id.sett_tv_end_time);

        mStartTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String time = mStartTimeTV.getText().toString();
                String [] components = time.split("(\\s|:)");

                int hour = Integer.valueOf(components[0]);
                int minute = Integer.valueOf(components[1]);
                if (components[2] == "pm") {
                    if (hour != 12) {
                        hour += 12;
                    }
                }

                TimePickerDialog timePicker;
                timePicker = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        setTimeInTextView(mStartTimeTV, selectedHour, selectedMinute);
                        mLookoutConfig.setStartTime(selectedHour, selectedMinute, true);
                    }
                }, hour, minute, false);
                timePicker.setTitle("Select Start Time");
                timePicker.show();
            }
        });

        mEndTimeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String time = mEndTimeTV.getText().toString();
                String [] components = time.split("(\\s|:)");

                int hour = Integer.valueOf(components[0]);
                int minute = Integer.valueOf(components[1]);
                if (components[2] == "pm") {
                    if (hour != 12) {
                        hour += 12;
                    }
                }

                TimePickerDialog timePicker;
                timePicker = new TimePickerDialog(mContext, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        setTimeInTextView(mEndTimeTV, selectedHour, selectedMinute);
                        mLookoutConfig.setEndTime(selectedHour, selectedMinute, true);
                    }
                }, hour, minute, false);
                timePicker.setTitle("Select End Time");
                timePicker.show();
            }
        });

        mTimeoutHoursSpinner = (Spinner) findViewById(R.id.sett_spinner_timeout_hours);
        mTimeoutMinutesSpinner = (Spinner) findViewById(R.id.sett_spinner_timeout_minutes);
        mTimeoutDemoToggleBtn = (ToggleButton) findViewById(R.id.sett_demo_toggle_btn);

        mTimeoutHoursSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (mInitComplete) {
                    mLookoutConfig.setInactivityTimeOut(Integer.parseInt(mTimeoutHoursSpinner.getSelectedItem().toString().split(" ")[0]),
                            Integer.parseInt(mTimeoutMinutesSpinner.getSelectedItem().toString().split(" ")[0]),
                            mTimeoutDemoToggleBtn.isChecked(), true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mTimeoutMinutesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (mInitComplete) {
                    mLookoutConfig.setInactivityTimeOut(Integer.parseInt(mTimeoutHoursSpinner.getSelectedItem().toString().split(" ")[0]),
                            Integer.parseInt(mTimeoutMinutesSpinner.getSelectedItem().toString().split(" ")[0]),
                            mTimeoutDemoToggleBtn.isChecked(), true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        mTimeoutDemoToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mInitComplete) {
                    if (mTimeoutDemoToggleBtn.isChecked()) {
                        mLookoutConfig.setInactivityTimeOut(0, 0, true, true);
                        mTimeoutHoursSpinner.setSelection(0);
                        mTimeoutMinutesSpinner.setSelection(0);
                    } else {
                        mLookoutConfig.setInactivityTimeOut(4, 0, false, true);
                        mTimeoutHoursSpinner.setSelection(4);
                        mTimeoutMinutesSpinner.setSelection(0);
                    }


                }
            }
        });

        mResetBtn = (Button) findViewById(R.id.sett_btn_reset);
        mResetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setMessage("Are you sure you want to reset to default values?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                setDefaultValues();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        mCheckConnectionThread = new CheckConnectionThread();
        mCheckConnectionThread.start();
    }

    private void updateUI(JSONObject jsonObject) {
        try {
            mMessageEditText.setText(jsonObject.getString("message"));
            mDisableEnableButton.setChecked(jsonObject.getBoolean("isActive"));
            mNumberListAdapter.setAll(jsonObject.getJSONArray("numbers"));

            String time = jsonObject.getString("startTime");
            String[] timeSplit = time.split(":");
            setTimeInTextView(mStartTimeTV, Integer.parseInt(timeSplit[0]), Integer.parseInt(timeSplit[1]));

            time = jsonObject.getString("endTime");
            timeSplit = time.split(":");
            setTimeInTextView(mEndTimeTV, Integer.parseInt(timeSplit[0]), Integer.parseInt(timeSplit[1]));

            String[] inactivityTimeout = jsonObject.getString("inactivityTimeout").split(":");
            mTimeoutHoursSpinner.setSelection(Integer.parseInt(inactivityTimeout[0]));
            mTimeoutMinutesSpinner.setSelection(Integer.parseInt(inactivityTimeout[1]));
            if (Integer.parseInt(inactivityTimeout[2]) == 10) {
                mTimeoutDemoToggleBtn.setChecked(true);
            } else {
                mTimeoutDemoToggleBtn.setChecked(false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setDefaultValues() {
        mLookoutConfig.setMessage("The Lookout: No movement detected.", false);
        mMessageEditText.setText("The Lookout: No movement detected.");

        mLookoutConfig.setIsLookoutActive(true, false);
        mDisableEnableButton.setChecked(true);

        mLookoutConfig.clearNumbers(false);
        mNumberListAdapter.clear();

        mStartTimeTV.setText("7:00 am");
        mEndTimeTV.setText("9:00 pm");
        mLookoutConfig.setStartTime(7, 0, false);
        mLookoutConfig.setEndTime(21, 0, false);

        mTimeoutHoursSpinner.setSelection(4);
        mTimeoutMinutesSpinner.setSelection(0);
        mTimeoutDemoToggleBtn.setChecked(false);
        mLookoutConfig.setInactivityTimeOut(Integer.parseInt(mTimeoutHoursSpinner.getSelectedItem().toString().split(" ")[0]),
                                            Integer.parseInt(mTimeoutMinutesSpinner.getSelectedItem().toString().split(" ")[0]),
                                            mTimeoutDemoToggleBtn.isChecked(), false);
        mLookoutConfig.sendToLookout();
    }

    private void setTimeInTextView(TextView textView, int selectedHour, int selectedMinute) {
        String dayNight;
        int hour = selectedHour;
        if (selectedHour >= 12) {
            dayNight = "pm";
            if (selectedHour != 12) {
                hour = selectedHour - 12;
            }
        } else {
            dayNight = "am";
            if (selectedHour == 0) {
                hour = 12;
            }
        }

        String time = hour + ":" + String.format("%02d", selectedMinute) + " " + dayNight;
        textView.setText(time);
    }

    private class CheckConnectionThread extends Thread {
        public CheckConnectionThread() {
        }

        public void run () {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog = ProgressDialog.show(mContext, "Connecting to Lookout Device", "Please wait...", true);
                }
            });
            startBluetoothConnection();
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //after connection keep checking to make sure the connection was not lost
            while (mBluetoothConnection != null) {
                if (!mBluetoothConnection.isConnected()) {
                    mBluetoothConnection = null;
                    break;
                }

                //check for a connection make sure server has not crashed
                mBluetoothConnection.sendData("checking connection".getBytes());

                if (mLookoutConfig.shouldUIUpdate()) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mLookoutConfig.setShouldUpdateUI(false);
                            updateUI(mLookoutConfig.getJSONObject());
                            mProgressDialog.dismiss();
                        }
                    });
                } else if (mLookoutConfig.shouldResetDefaults()){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLookoutConfig.setShouldResetDefaults(false);
                            setDefaultValues();
                            mProgressDialog.dismiss();
                        }
                    });
                }
                try {
                    sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "Lost connection to lookout server, returning to main activity");
            sendToast("Lost connection to Lookout Server...");
            finishActivity();
        }
    }

    private void startBluetoothConnection() {
        if (mLookoutDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            mBluetoothConnection = new BluetoothConnection(mContext, mLookoutConfig);
            mBluetoothConnection.StartClientConnection(mLookoutDevice, mLookoutUUID);
            while (!mBluetoothConnection.isConnected() && !mBluetoothConnection.hasFailedConnection());
            if (mBluetoothConnection.hasFailedConnection()) {
                sendToast("Bluetooth connection Failed, please try again...");
                Log.d(TAG, "Failed to connect to " + mLookoutDevice.getName());
                finishActivity();
            } else {
                mLookoutConfig.setBluetoothConnectionObject(mBluetoothConnection);
                String request = "request current settings";
                mBluetoothConnection.sendData(request.getBytes());
                try{
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.dismiss();
                            mProgressDialog = ProgressDialog.show(mContext, "Requesting settings from the Lookout", "Please wait...", true);
                        }
                    });
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
            }
        } else {
            Log.d(TAG, "Lost bond to device: " + mLookoutDevice.getName());
            finishActivity();
        }
    }

    private void sendToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run()
            {
                Toast toast = Toast.makeText(mContext, msg, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }
}
