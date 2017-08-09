package io.kiny;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

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
    @SuppressLint("StaticFieldLeak")
    private static Context mApplicationContext;

    private static List<String> openDoors = null;
    private static Queue<LockerCommand> commandQueue = new ConcurrentLinkedDeque<>();
    private static LockerCommand currentCommand = null;

    private static class LockerResponseHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_CONNECTION_LOST:
                    Log.d("LockerManager", "connection lost");
                    // reconnect since connection is lost
                    if (mBluetoothClient != null) {
                        mBluetoothClient.connect();
                    }
                    break;
                case Constants.MESSAGE_CONNECTED:
                    Log.d("LockerManager", "connected");
                    // query open doors if don't know it yet.
                    if (openDoors == null) {
                        queryDoorStatus(null, true);
                    }
                    break;
                case Constants.MESSAGE_INCOMING_MESSAGE:
                    String message = (String) msg.obj;
                    Log.d("LockerManager", String.format("incoming response:%s", message));
                    break;
            }
        }
    }

    // need reference to application context as LockerManager will live longer than the activity
    public LockerManager(String targetDeviceName, Context applicationContext, boolean useSimulator) {
        mApplicationContext = applicationContext;
        LockerResponseHandler handler = new LockerResponseHandler();
        if (useSimulator) {
            mBluetoothClient = new FakeBTClient(handler, true);
        } else {
            mBluetoothClient = new BluetoothClient(handler, targetDeviceName);
        }
    }

    public static void start() {
        if (mBluetoothClient != null && mBluetoothClient.getState() == BluetoothClient.STATE_NONE) {
            mBluetoothClient.connect();
            mBluetoothClient.getBluetoothBroadcastReceiver()
                    .safeRegister(mApplicationContext, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        }
        commandQueue.clear();
    }

    public static void stop() {
        if (mBluetoothClient != null) {
            mBluetoothClient.disconnect();
            mBluetoothClient.getBluetoothBroadcastReceiver().safeUnregister(mApplicationContext);
        }
        commandQueue.clear();
        // clear open doors so that the next time it gets connected, it will re-query the door status.
        openDoors = null;
        currentCommand = null;
    }

    private static boolean isBtConnected() {
        return mBluetoothClient != null && mBluetoothClient.getState() == BluetoothClientInterface.STATE_CONNECTED;
    }

    public static void queryDoorStatus(Collection<String> doors, boolean allDoor) {
        LockerCommand command = new LockerCommand(LockerCommandType.DoorStatus, doors, allDoor);
        processCommand(command);
    }

    private static synchronized void processCommand(LockerCommand command) {
        //using synchronized to protect currentCommand
        if (currentCommand == null && isBtConnected()) {
            currentCommand = command;
            Log.d("LockerManager", String.format("sending command:%s", command.toString()));
            mBluetoothClient.sendCommand(command.toString());
            command.recordSent();
        } else {
            commandQueue.offer(command);
        }
    }

    public static void requestToCheckIn(String compartmentNumber) {
        if (isBtConnected()) {
            Collection<String> doors = Collections.singletonList(compartmentNumber);
            LockerCommand command = new LockerCommand(LockerCommandType.CheckIn, doors, true);
            processCommand(command);
        }
    }
}
