package com.canassist.a499.lookout;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    //Config
    private static final String TAG = "[MainActivity]";
    private static final String DEVICE_NAME = "Coyote"; //name of lookout device
    private static final int REQUEST_ENABLE_BT = 1;

    //Variables
    private Context mContext;
    private Handler mHandler;
    private ProgressBar mProgressBar;
    private BluetoothAdapter mBluetoothAdapter;
    private Button mConnectButton;

    //States
    private static final int BT_DETECTING = 0;
    private static final int BT_ENABLING = 1;
    private static final int BT_FINDING = 2;
    private static final int BT_PAIRING = 3;
    private static final int BT_READY_FOR_CONNECTION = 4;

    //State Flow conditions
    private int curr_state = BT_DETECTING;
    private boolean mEnablingBluetooth = false;
    private boolean mIsDiscovery = false;

    //Bluetooth connection information
    private BluetoothDevice mLookoutDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Starting application");
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy Called");
        unregisterReceiver(mReceiver);
    }

    private void init() {
        mContext = getApplicationContext();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mConnectButton = (Button) findViewById(R.id.main_bt_connect_btn);
        mConnectButton.setEnabled(false);

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSettingsActivity();
            }
        });

        mHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message message) {

                ImageView image;
                switch (message.arg1) {
                    case BT_DETECTING:
                        findViewById(R.id.main_bt_progress_spinner_enable).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_find).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_pair).setVisibility(ProgressBar.INVISIBLE);
                        mProgressBar = (ProgressBar) findViewById(R.id.main_bt_progress_spinner_detected);
                        if (mProgressBar.getVisibility() != ProgressBar.VISIBLE) {
                            mProgressBar.setVisibility(ProgressBar.VISIBLE);
                        }

                        image = (ImageView) findViewById(R.id.main_bt_detected_img);
                        image.setImageResource(R.drawable.failed_icon);
                        break;
                    case BT_ENABLING:
                        findViewById(R.id.main_bt_progress_spinner_detected).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_find).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_pair).setVisibility(ProgressBar.INVISIBLE);
                        mProgressBar = (ProgressBar) findViewById(R.id.main_bt_progress_spinner_enable);
                        if (mProgressBar.getVisibility() != ProgressBar.VISIBLE) {
                            mProgressBar.setVisibility(ProgressBar.VISIBLE);
                        }

                        image = (ImageView) findViewById(R.id.main_bt_detected_img);
                        image.setImageResource(R.drawable.success_icon);
                        image = (ImageView) findViewById(R.id.main_bt_enabled_img);
                        image.setImageResource(R.drawable.failed_icon);
                        image = (ImageView) findViewById(R.id.main_bt_lookout_found_img);
                        image.setImageResource(R.drawable.failed_icon);
                        image = (ImageView) findViewById(R.id.main_bt_paired_img);
                        image.setImageResource(R.drawable.failed_icon);
                        mConnectButton.setEnabled(false);
                        break;
                    case BT_FINDING:
                        findViewById(R.id.main_bt_progress_spinner_detected).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_enable).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_pair).setVisibility(ProgressBar.INVISIBLE);
                        mProgressBar = (ProgressBar) findViewById(R.id.main_bt_progress_spinner_find);
                        if (mProgressBar.getVisibility() != ProgressBar.VISIBLE) {
                            mProgressBar.setVisibility(ProgressBar.VISIBLE);
                        }

                        image = (ImageView) findViewById(R.id.main_bt_enabled_img);
                        image.setImageResource(R.drawable.success_icon);
                        image = (ImageView) findViewById(R.id.main_bt_lookout_found_img);
                        image.setImageResource(R.drawable.failed_icon);
                        image = (ImageView) findViewById(R.id.main_bt_paired_img);
                        image.setImageResource(R.drawable.failed_icon);
                        mConnectButton.setEnabled(false);
                        break;
                    case BT_PAIRING:
                        findViewById(R.id.main_bt_progress_spinner_detected).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_enable).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_find).setVisibility(ProgressBar.INVISIBLE);
                        mProgressBar = (ProgressBar) findViewById(R.id.main_bt_progress_spinner_pair);
                        if (mProgressBar.getVisibility() != ProgressBar.VISIBLE) {
                            mProgressBar.setVisibility(ProgressBar.VISIBLE);
                        }

                        image = (ImageView) findViewById(R.id.main_bt_lookout_found_img);
                        image.setImageResource(R.drawable.success_icon);
                        image = (ImageView) findViewById(R.id.main_bt_paired_img);
                        image.setImageResource(R.drawable.failed_icon);
                        mConnectButton.setEnabled(false);
                        break;
                    case BT_READY_FOR_CONNECTION:
                        findViewById(R.id.main_bt_progress_spinner_detected).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_enable).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_find).setVisibility(ProgressBar.INVISIBLE);
                        findViewById(R.id.main_bt_progress_spinner_pair).setVisibility(ProgressBar.INVISIBLE);
                        image = (ImageView) findViewById(R.id.main_bt_paired_img);
                        image.setImageResource(R.drawable.success_icon);
                        mConnectButton.setEnabled(true);
                        break;
                    default:
                        break;
                }

            }
        };

        Runnable runnable = new Runnable() {
            public void run () {
                try {
                    Looper.prepare();
                    View view = getWindow().getDecorView().findViewById(android.R.id.content);
                    View currentView = view;

                    do {
                        //Reset Cases
                        if (curr_state > BT_ENABLING && !mBluetoothAdapter.isEnabled()) {
                            curr_state = BT_ENABLING;
                            sendToast("Bluetooth adapter was disabled restarting...");
                        }

                        //update progress bar states
                        switch (curr_state) {
                            case BT_DETECTING:
                                sendHandlerMessage(BT_DETECTING);

                                //check that the device supports bluetooth
                                if (mBluetoothAdapter == null) {
                                    sendToast("A Bluetooth adapter has not been detected.");
                                    finish();
                                } else {
                                    curr_state = BT_ENABLING;
                                }
                                break;
                            case BT_ENABLING:
                                sendHandlerMessage(BT_ENABLING);

                                //Enable the bluetooth adapter
                                if (!mBluetoothAdapter.isEnabled()) {
                                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                    if (!mEnablingBluetooth) {
                                        startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
                                        mEnablingBluetooth = true;
                                    }
                                    mIsDiscovery = false;
                                    mLookoutDevice = null;

                                    if (mBluetoothAdapter.isDiscovering()) {
                                        mBluetoothAdapter.cancelDiscovery();
                                    }
                                }
                                else {
                                    curr_state = BT_FINDING;
                                }
                                break;
                            case BT_FINDING:
                                sendHandlerMessage(BT_FINDING);
                                if (!mIsDiscovery && mLookoutDevice == null) {
                                    Log.d(TAG, "Discovering devices...");
                                    discoverDevices();
                                } else if (mIsDiscovery && !mBluetoothAdapter.isDiscovering()) {
                                    Log.d(TAG, "Failed to discover Lookout Device, restarting scan...");
                                    discoverDevices();
                                }

                                if (mLookoutDevice != null) {
                                    curr_state = BT_PAIRING;
                                }
                                break;
                            case BT_PAIRING:
                                sendHandlerMessage(BT_PAIRING);

                                if (mLookoutDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                                    curr_state = BT_READY_FOR_CONNECTION;
                                }
                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                    if (mLookoutDevice.getBondState() != BluetoothDevice.BOND_BONDING) {
                                        Log.d(TAG, "Attempting to pair with device: " + mLookoutDevice.getName());
                                        mLookoutDevice.createBond();
                                    }
                                } else {
                                    sendToast("Android build version less than Jelly Bean MR2");
                                }
                                break;
                            case BT_READY_FOR_CONNECTION:
                                sendHandlerMessage(BT_READY_FOR_CONNECTION);
                                break;
                            default:
                                break;
                        }
                        view = getWindow().getDecorView().findViewById(android.R.id.content);
                    } while(currentView == view);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Looper.loop();
            }
        };

        new Thread(runnable).start();
    }

    private void discoverDevices() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "Canceling device discovery");
        }
        //Depending on android version a check is needed
        checkBluetoothPermissions();
        mBluetoothAdapter.startDiscovery();
        sendToast("Discovering Lookout Device");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        mIsDiscovery = true;
    }

    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            }
        } else {
            Log.d(TAG, "Version is less than Lollipop no permission check is needed.");
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                if (device.getName().equalsIgnoreCase(DEVICE_NAME)) {
                    mLookoutDevice = device;
                    Log.d(TAG, "Found Lookout Device: " + device.getName() + " " + device.getAddress());
                }
                mBluetoothAdapter.cancelDiscovery();
                mIsDiscovery = false;
            }
        }
    };

    private void sendHandlerMessage(int arg1) {
        Message message = mHandler.obtainMessage();
        message.arg1 = arg1;
        message.sendToTarget();
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

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_CANCELED) {
            sendToast("Bluetooth must be enabled to continue");
        } else if (resultCode != RESULT_OK) {
            sendToast("Error enabling Bluetooth, try again");
        }
        mEnablingBluetooth = false;
    }

    private void startSettingsActivity() {
        sendToast("Starting Connection...");
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("LOOKOUT_DEVICE", mLookoutDevice);
        this.startActivity(intent);
    }

}
