package io.kiny;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
    private static TimeoutCommandTriggerThread timeoutCommandTriggerThread = null;

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
                        queryBoxStatus(null);
                    }
                    break;
                case Constants.MESSAGE_INCOMING_MESSAGE:
                    String message = (String) msg.obj;
                    Log.d("LockerManager", String.format("incoming response:%s", message));
                    // Dequeue the current command if its ID matches the command ID in the response.
                    // If ack is received after a check in/ checkout command, mark the door as open.
                    // If the queue becomes empty, and there's open door, enqueue a door query command for the opened doors.
                    if (message.matches(LockerResponse.RESPONSE_PATTERN)) {
                        LockerResponse response = new LockerResponse(message);
                        if (currentCommand != null && Objects.equals(response.getId(), currentCommand.getId())) {
                            //remove the current command and dequeue
                            currentCommand = commandQueue.poll();
                            if (currentCommand == null && openDoors != null && openDoors.size() > 0) {
                                queueCommand(new LockerCommand(LockerCommandType.BoxStatus, openDoors));
                            }//else, the current command will be fired on the next loop.
                        }
                    }
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
        commandQueue.clear();
        if (timeoutCommandTriggerThread != null) {
            timeoutCommandTriggerThread.cancel();
        }
        timeoutCommandTriggerThread = new TimeoutCommandTriggerThread();
        timeoutCommandTriggerThread.start();
    }

    public void stop() {
        if (mBluetoothClient != null) {
            mBluetoothClient.disconnect();
            mBluetoothClient.getBluetoothBroadcastReceiver().safeUnregister(mApplicationContext);
        }
        commandQueue.clear();
        // clear open doors so that the next time it gets connected, it will re-query the door status.
        openDoors = null;
        currentCommand = null;
        if (timeoutCommandTriggerThread != null) {
            timeoutCommandTriggerThread.cancel();
            timeoutCommandTriggerThread = null;
        }
    }

    private static boolean isBtConnected() {
        return mBluetoothClient != null && mBluetoothClient.getState() == BluetoothClientInterface.STATE_CONNECTED;
    }

    public static void queryBoxStatus(List<String> boxes) {
        LockerCommand command = new LockerCommand(LockerCommandType.BoxStatus, boxes);
        queueCommand(command);
    }

    private static synchronized void queueCommand(LockerCommand command) {
        //using synchronized to protect currentCommand
        if (currentCommand == null) {
            currentCommand = command;
        } else {
            commandQueue.offer(command);
        }
    }

    private static void sendCommand(LockerCommand command) {
        //intentionally drop the command if disconnected,
        if (isBtConnected()) {
            Log.d("LockerManager", String.format("sending command:%s", command.toString()));
            mBluetoothClient.sendCommand(command.toString());
        }
        // so that the queue can always be consumed fast.
        command.recordSent();
    }

    public static void requestToCheckIn(String compartmentNumber) {
        if (isBtConnected()) {
            List<String> doors = Collections.singletonList(compartmentNumber);
            LockerCommand command = new LockerCommand(LockerCommandType.CheckIn, doors);
            queueCommand(command);
        }
    }


    /**
     * Constant loop and check whether the current command expired by compare the command's sent time and now.
     * If the current command expired, remove it and send the next command in queue to the board.
     * Todo: maybe able to optimize CPU usage by killing this thread when not needed and only let it run when it becomes necessary
     */
    private class TimeoutCommandTriggerThread extends Thread {
        private boolean mmStopSignal = false;

        public void run() {
            while (!mmStopSignal) {
                try {
                    if (currentCommand != null) {
                        if (currentCommand.getSent() == null) {
                            sendCommand(currentCommand);
                        } else if (currentCommand.expired()) {
                            currentCommand = commandQueue.poll();
                            if (currentCommand != null)
                                sendCommand(currentCommand);
                        }
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            mmStopSignal = true;
        }
    }

}
