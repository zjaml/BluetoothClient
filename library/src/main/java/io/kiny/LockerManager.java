package io.kiny;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

import java.util.Locale;

import io.kiny.bluetooth.BluetoothClient;
import io.kiny.bluetooth.BluetoothClientInterface;
import io.kiny.bluetooth.Constants;
import io.kiny.bluetooth.FakeBTClient;

/**
 * Created by JZhao on 8/9/2017.
 * Manage Locker
 */

public class LockerManager {
    private static BluetoothClientInterface mBluetoothClient;
    private Context mApplicationContext;

    private static class LockerResponseHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_CONNECTION_LOST:
                    // reconnect since connection is lost
                    if (mBluetoothClient != null) {
                        mBluetoothClient.connect();
                    }
                    break;
                case Constants.MESSAGE_CONNECTED:
                    break;
                case Constants.MESSAGE_INCOMING_MESSAGE:
                    String message = (String) msg.obj;
                    break;
            }
        }
    }

    // need reference to application context as LockerManager will live longer than the activity
    public LockerManager(String targetDeviceName, Context applicationContext, boolean useSimulator) {
        mApplicationContext = applicationContext;
        LockerResponseHandler handler = new LockerResponseHandler();
        if (useSimulator) {
            mBluetoothClient = new FakeBTClient(handler, false);
        } else {
            mBluetoothClient = new BluetoothClient(handler, targetDeviceName);
        }
    }

    public void start() {
        if (mBluetoothClient != null && mBluetoothClient.getState() == BluetoothClient.STATE_NONE) {
            mBluetoothClient.connect();
            mBluetoothClient.getBluetoothBroadcastReceiver()
                    .safeRegister(mApplicationContext, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        }
    }

    public void stop() {
        if (mBluetoothClient != null) {
            mBluetoothClient.disconnect();
            mBluetoothClient.getBluetoothBroadcastReceiver().safeUnregister(mApplicationContext);
        }
    }

    private boolean isBtConnected() {
        return mBluetoothClient != null && mBluetoothClient.getState() == BluetoothClientInterface.STATE_CONNECTED;
    }

    public void RequestToCheckin(int compartmentNumber) {
        if (isBtConnected()) {
            String command = String.format(Locale.getDefault(), "O%02dT", compartmentNumber);
            mBluetoothClient.sendCommand(command);
        }
    }
}
