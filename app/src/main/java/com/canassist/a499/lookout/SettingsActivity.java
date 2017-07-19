package com.canassist.a499.lookout;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

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
    private ImageButton mAddNumber;
    private ContactListAdapter mNumberListAdapter;
    private ListView mNumberListView;
    private Spinner mTimeoutHours;
    private Spinner mTimeoutMinutes;
    private Spinner mTimeoutSeconds;
    private Button mStartTimeBtn;
    private Button mEndTimeBtn;
    private TextView mStartTimeTV;
    private TextView mEndTimeTV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        init();
    }

    private void init() {
        mContext = SettingsActivity.this;
        mLookoutDevice = getIntent().getParcelableExtra("LOOKOUT_DEVICE");

        TextView tv = (TextView) findViewById(R.id.set_tv_dev_name);
        tv.setText(mLookoutDevice.getName());

        mAddNumber = (ImageButton) findViewById(R.id.add_number_btn);
        mAddNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNumberListAdapter.createNewNumberField();
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
                    }
                }, hour, minute, false);
                timePicker.setTitle("Select End Time");
                timePicker.show();
            }
        });

        mCheckConnectionThread = new CheckConnectionThread(mBluetoothConnection);
        mCheckConnectionThread.start();
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

        //TODO: send as JSON
        //send to lookout device
    }

    private class CheckConnectionThread extends Thread {
        private BluetoothConnection mBluetoothConnection;

        public CheckConnectionThread(BluetoothConnection bluetoothConnection) {
            mBluetoothConnection = bluetoothConnection;
        }

        public void run () {
            startBluetoothConnection();
            while (mBluetoothConnection != null) {
                if (!mBluetoothConnection.isConnected()) {
                    mBluetoothConnection = null;
                    Log.d(TAG, "Lost connection to lookout server, returning to main activity");
                    sendToast("Lost connection to Lookout Server...");
                    finish();
                }

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startBluetoothConnection() {
        if (mLookoutDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
            mBluetoothConnection = new BluetoothConnection(mContext);
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog = ProgressDialog.show(mContext, "Connecting to Lookout Device", "Please wait...", true);
                }
            });
            mBluetoothConnection.StartClientConnection(mLookoutDevice, mLookoutUUID);
            while (!mBluetoothConnection.isConnected() && !mBluetoothConnection.hasFailedConnection());
            if (mBluetoothConnection.hasFailedConnection()) {
                sendToast("Bluetooth connection Failed, please try again...");
                Log.d(TAG, "Failed to connect to " + mLookoutDevice.getName());
                finish();
            } else {
                mLookoutConfig.setBluetoothConnectionObject(mBluetoothConnection);
                try{
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mProgressDialog.dismiss();
                        }
                    });
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
            }

        } else {
            Log.d(TAG, "Lost bond to device: " + mLookoutDevice.getName());
            finish();
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
