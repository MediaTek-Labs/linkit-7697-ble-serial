/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.ble_serial;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.mediatek.wearable.Controller;
import com.mediatek.wearable.WearableListener;
import com.mediatek.wearable.WearableManager;
import com.mediatek.ctrl.fota.common.FotaOperator;
import com.mediatek.ctrl.fota.common.FotaVersion;
import com.mediatek.ctrl.fota.common.IFotaOperatorCallback;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;
    private TextView mReceiveField;
    private EditText mSendField;
    private Button mSendBtn;
    private boolean mConnected = false;

    private SerialController mController;

    private void clearUI()
    {
        mReceiveField.setText("");
    }

    private WearableListener mWearableListener = new WearableListener() {

        @Override
        public void onDeviceChange(BluetoothDevice device) {
        }

        @Override
        public void onConnectChange(int oldState, int newState) {

            if (newState == WearableManager.STATE_CONNECTED) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            }
            else if (newState == WearableManager.STATE_CONNECT_LOST) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            }
        }


        @Override
        public void onDeviceScan(BluetoothDevice device) {
        }

        @Override
        public void onModeSwitch(int newMode) {
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mReceiveField = (TextView) findViewById(R.id.receive_value);
        mSendField = (EditText) findViewById(R.id.send_value);
        mSendBtn = (Button)findViewById(R.id.button_send);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        boolean isSuccess = WearableManager.getInstance().init(true, getApplicationContext(), null, 0);
        Log.d(TAG, "WearableManager init " + isSuccess);

        // make sure in DOGP mode
        if (WearableManager.getInstance().getWorkingMode() != WearableManager.MODE_DOGP) {
            WearableManager.getInstance().switchMode();
        }
        WearableManager.getInstance().registerWearableListener(mWearableListener);
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mDeviceAddress);
        WearableManager.getInstance().setRemoteDevice(device);
        WearableManager.getInstance().connect();

        mController = new SerialController();
        WearableManager.getInstance().addController(mController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            mSendBtn.setEnabled(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            mSendBtn.setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                WearableManager.getInstance().connect();
                return true;
            case R.id.menu_disconnect:
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    public void onClickSend(View view) {
        if(mConnected)
        {
            String data = mSendField.getText().toString().trim();
            if(!data.equals(""))
                mController.sendSerialData(data.getBytes());
        }
    }

    public class SerialController extends Controller {
        public static final String CMD_SENDER = "myserial";
        public static final String CMD_RECEIVER = "myserial";

        public SerialController() {
            super(CMD_SENDER, CMD_9);
            HashSet<String> receivers = new HashSet<String>();
            receivers.add(CMD_RECEIVER);
            super.setReceiverTags(receivers);
        }

        public void sendSerialData(byte[] data) {
            String cmd = CMD_SENDER + " " + CMD_SENDER + " 0 0 " + data.length + " ";
            try {
                super.send(cmd, data, false, false, PRIORITY_NORMAL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void onReceive(byte[] data) {
            String receiveCmd = new String(data);
            String[] commands = receiveCmd.split(" ");

            Log.d(TAG, "onReceive: " + receiveCmd);

            if (!commands[1].equals(CMD_RECEIVER))
                return;

            Calendar c = Calendar.getInstance();
            int HH = c.get(Calendar.HOUR_OF_DAY);
            int MM = c.get(Calendar.MINUTE);
            int SS = c.get(Calendar.SECOND);
            mReceiveField.append(String.format("[%02d:%02d:%02d] %s\n", HH, MM, SS, commands[4]));
        }
    }
}
