package com.canassist.a499.lookout;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnection implements Serializable {
    private static final String TAG = "[BluetoothConnection]";
    private Context mContext;
    private LookoutConfig mLookoutConfig;
    private BluetoothAdapter mBluetoothAdapter;

    private ConnectionThread mConnectionThread;
    private ConnectedCommunicationThread mConnectedCommunicationThread;
    private BluetoothDevice mDevice;
    private UUID mdeviceUUID;

    private boolean mIsConnected = false;
    private boolean mHasFailedConnection = false;

    public BluetoothConnection(Context context, LookoutConfig lookoutConfig) {
        mContext = context;
        mLookoutConfig = lookoutConfig;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "Starting Bluetooth connnection");
    }

    public ConnectionThread getConnectionThread() {
        return mConnectionThread;
    }

    public ConnectedCommunicationThread getConnectedCommunicationThread() {
        return mConnectedCommunicationThread;
    }

    public void sendData(byte [] data) {
        Log.d(TAG, "Sending data to " + mDevice.getName());
        mConnectedCommunicationThread.sendData(data);
    }

    public void StartClientConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "Starting a client connection");
        mConnectionThread = new ConnectionThread(device, uuid);
        mConnectionThread.start();
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    public boolean hasFailedConnection() {
        return mHasFailedConnection;
    }

    public class ConnectionThread extends Thread {
        BluetoothSocket mSocket;
        String CT_TAG = "[Connect]";

        public ConnectionThread(BluetoothDevice device, UUID uuid) {
            mDevice = device;
            mdeviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;

            //Try to make a RFCOMM socket
            try {
                tmp = mDevice.createRfcommSocketToServiceRecord(mdeviceUUID);
            } catch (IOException e) {
                Log.e(CT_TAG, "Failed to create an RTCOMM Socket to service record: \n" + e.getMessage());
            }

            mSocket = tmp;
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }

            //Connect to the bluetooth lookout device
            try {
                mSocket.connect();
                Log.i(CT_TAG, "Connected to Lookout Device");
                connected(mSocket);
            } catch (IOException e) {
                // Close the socket
                try {
                    mSocket.close();
                    Log.d(CT_TAG, "Closing Socket");
                } catch (IOException e1) {
                    Log.e(CT_TAG, "Unable to close connection in socket " + e1.getMessage());
                }
                mHasFailedConnection = true;
                Log.d(CT_TAG, "Could not connect to the Lookout Device through UUID: " + mdeviceUUID);
            }
        }

        public void cancel() {
            try {
                Log.d(TAG, "Canceling ConnectionThread");
                mSocket.close();
                mIsConnected = false;
            } catch (IOException e) {
                Log.e(TAG, "Failed to close ConnectedThread socket");
            }
        }
    }

    public class ConnectedCommunicationThread extends Thread {

        private BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedCommunicationThread(BluetoothSocket socket) {
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];

            int bytes;
            while (true) {

                try {
                    bytes = mInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    if (incomingMessage.length() != 0) {
                        if (incomingMessage.equalsIgnoreCase("connection exists")) {
                            mIsConnected = true;
                        } else {
                            mLookoutConfig.receivedSettings(incomingMessage);
                        }
                    }
                    if (!mSocket.isConnected()) {
                        cancel();
                        break;
                    }
                    Log.d(TAG, "InputStream: " + incomingMessage);
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    break;
                }
            }
        }

        public void sendData(byte[] data) {
            String text = new String(data, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                if (new String(data, "UTF-8").equalsIgnoreCase("checking connection")) {
                    mIsConnected = false;
                }
                mOutStream.write(data);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
            }
        }

        public void cancel() {
            try {
                Log.d(TAG, "Canceling Connected Communication Thread");
                mSocket.close();
                mIsConnected = false;
            } catch (IOException e) {
                Log.e(TAG, "Failed to close Connected Communication Thread socket");
            }
        }
    }

    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        mConnectedCommunicationThread = new ConnectedCommunicationThread(mmSocket);
        mConnectedCommunicationThread.start();
        mIsConnected = true;
    }

}
