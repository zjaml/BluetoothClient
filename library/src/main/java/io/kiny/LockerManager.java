package io.kiny;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private BluetoothClientInterface mBluetoothClient;
    private boolean _useSimulator;
    private String _targetDeviceName;
    private Context mApplicationContext;
    private Map<String, BoxStatus> boxStatusMap = null;
    //    private static List<BoxStatus> boxStatusList = null;
    private Queue<LockerCommand> commandQueue;
    private LockerCommand currentCommand = null;
    private CommanderThread commanderThread = null;
    public static final String ACTION_LOCKER_READY = "LOCKER_ALL_BOXES_STATUS";
    public static final String ACTION_LOCKER_CONNECTED = "LOCKER_CONNECTED";
    public static final String ACTION_LOCKER_DISCONNECTED = "LOCKER_DISCONNECTED";
    public static final String ACTION_LOCKER_BOX_CLOSED = "LOCKER_BOX_CLOSED";
    public static final String ACTION_LOCKER_BOX_OPENED = "LOCKER_BOX_OPENED";
    public static final String ACTION_LOCKER_CHARGING = "LOCKER_CHARGING";
    public static final String ACTION_LOCKER_DISCHARGING = "LOCKER_DISCHARGING";

    private class LockerResponseHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_CONNECTION_LOST: {
                    Log.d("LockerManager", "connection lost");
                    Intent intent = new Intent(ACTION_LOCKER_DISCONNECTED);
                    LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
                    // reconnect since connection is lost
                    if (mBluetoothClient != null) {
                        mBluetoothClient.connect();
                    }
                    break;
                }
                case Constants.MESSAGE_CONNECTED: {
                    Log.d("LockerManager", "connected");
                    queryBoxStatus(null);
                    Intent intent = new Intent(ACTION_LOCKER_CONNECTED);
                    LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
                    break;
                }
                case Constants.MESSAGE_INCOMING_MESSAGE: {
                    String message = (String) msg.obj;
                    Log.d("LockerManager", String.format("incoming response:%s", message));
                    // Dequeue the current command if its ID matches the command ID in the response.
                    // If ack is received after a check in/ checkout command, mark the door as open.
                    // If the queue becomes empty, and there's open door, enqueue a door query command for the opened doors.
                    if (message.matches(LockerResponse.RESPONSE_PATTERN)) {
                        LockerResponse response = new LockerResponse(message);
                        if (Objects.equals(response.getType(), LockerResponse.RESPONSE_TYPE_CHARGING)) {
                            Intent intent = new Intent(ACTION_LOCKER_CHARGING);
                            LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
                        }
                        if (Objects.equals(response.getType(), LockerResponse.RESPONSE_TYPE_DISCHARGING)) {
                            Intent intent = new Intent(ACTION_LOCKER_DISCHARGING);
                            LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
                        }
                        // monitor box status change, emit door closed event.
                        List<BoxStatus> boxStatusList = response.getBoxStatus();
                        if (boxStatusList != null && boxStatusList.size() > 0) {
                            updateBoxStatus(boxStatusList);
                        }
                        // update box status
                        if (currentCommand != null && Objects.equals(response.getId(), currentCommand.getId())) {
                            //remove the current command and dequeue
                            currentCommand = commandQueue.poll();
                        }
                        List<String> openBoxes = getOpenBoxes();
                        if (currentCommand == null && openBoxes.size() > 0) {
                            queueCommand(new LockerCommand(LockerCommand.COMMAND_TYPE_BOX_STATUS, openBoxes));
                        }//else, the current command will be fired on the next loop.
                    }
                    break;
                }
            }
        }
    }

    // need reference to application context as LockerManager will live longer than the activity
    public LockerManager(String targetDeviceName, Context applicationContext, boolean useSimulator) {
        mApplicationContext = applicationContext;
        _useSimulator = useSimulator;
        _targetDeviceName = targetDeviceName;
    }

    private List<String> getOpenBoxes() {
        List<String> boxes = new ArrayList<>();
        if (boxStatusMap == null)
            return boxes;
        Collection<BoxStatus> boxStatusList = boxStatusMap.values();
        for (BoxStatus boxStatus : boxStatusList) {
            if (Objects.equals(boxStatus.getStatus(), BoxStatus.BOX_OPEN)) {
                boxes.add(boxStatus.getBoxNumber());
            }
        }
        return boxes;
    }

    private void updateBoxStatus(List<BoxStatus> boxStatusList) {
        for (BoxStatus newStatus : boxStatusList) {
            if (boxStatusMap == null) {
                boxStatusMap = new HashMap<>();
            }
            if (boxStatusMap.containsKey(newStatus.getBoxNumber())) {
                BoxStatus old = boxStatusMap.get(newStatus.getBoxNumber());
                if (!Objects.equals(old.getStatus(), newStatus.getStatus())) {
                    if (Objects.equals(newStatus.getStatus(), BoxStatus.BOX_OPEN)) {
                        Log.d("LockerManager", String.format("Box %s opened!", newStatus.getBoxNumber()));
                        Intent intent = new Intent(ACTION_LOCKER_BOX_OPENED);
                        intent.putExtra("box", newStatus.getBoxNumber());
                        LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
                    } else {
                        Log.d("LockerManager", String.format("Box %s closed, %s!", newStatus.getBoxNumber(), newStatus.getStatus()));
                        Intent intent = new Intent(ACTION_LOCKER_BOX_CLOSED);
                        intent.putExtra("box", newStatus.getBoxNumber());
                        intent.putExtra("status", newStatus.getStatus());
                        LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
                    }
                }
            }
            boxStatusMap.put(newStatus.getBoxNumber(), newStatus);
        }
        if (boxStatusList.size() == 30) {
            Intent intent = new Intent(ACTION_LOCKER_READY);
            LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
        }
    }

    public void start() {
        LockerResponseHandler handler = new LockerResponseHandler();
        boxStatusMap = new HashMap<>();
        if (_useSimulator) {
            mBluetoothClient = new FakeBTClient(handler, false);
        } else {
            mBluetoothClient = new BluetoothClient(handler, _targetDeviceName);
        }
        mBluetoothClient.connect();
        mBluetoothClient.getBluetoothBroadcastReceiver()
                .safeRegister(mApplicationContext, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        commandQueue = new ConcurrentLinkedDeque<>();
        commanderThread = new CommanderThread();
        commanderThread.start();
    }

    public void stop() {
        if (mBluetoothClient != null) {
            mBluetoothClient.disconnect();
            mBluetoothClient.getBluetoothBroadcastReceiver().safeUnregister(mApplicationContext);
            mBluetoothClient = null;
        }
        commandQueue.clear();
        commandQueue = null;
        // clear open doors so that the next time it gets connected, it will re-query the door status.
        boxStatusMap = null;
        currentCommand = null;
        if (commanderThread != null) {
            commanderThread.cancel();
            commanderThread = null;
        }
    }

    public boolean isBtConnected() {
        return mBluetoothClient != null && mBluetoothClient.getState() == BluetoothClientInterface.STATE_CONNECTED;
    }

    public Collection<BoxStatus> getBoxStatus() {
        return boxStatusMap.values();
    }

    private synchronized void queueCommand(LockerCommand command) {
        //using synchronized to protect currentCommand
        if (currentCommand == null) {
            currentCommand = command;
        } else {
            commandQueue.offer(command);
        }
    }

    public boolean isReady() {
        return boxStatusMap != null && boxStatusMap.values().size() == 30;
    }

    public void queryBoxStatus(List<String> boxes) {
        LockerCommand command = new LockerCommand(LockerCommand.COMMAND_TYPE_BOX_STATUS, boxes);
        queueCommand(command);
    }

    private void sendCommand(LockerCommand command) {
        //intentionally drop the command if disconnected,
        if (isBtConnected()) {
            Log.d("LockerManager", String.format("sending command:%s", command.toString()));
            mBluetoothClient.sendCommand(command.toString());
        }
        // so that the queue can always be consumed fast.
        command.recordSent();
    }

    public void requestToCheckIn(String compartmentNumber) {
        if (!isReady()) {
            // the locker manager must know all box status to proceed.
            queryBoxStatus(null);
            return;
        }
        if (isBtConnected()) {
            List<String> boxes = Collections.singletonList(compartmentNumber);
            LockerCommand command = new LockerCommand(LockerCommand.COMMAND_TYPE_CHECK_IN, boxes);
            queueCommand(command);
        }
    }

    public void getReady() {
        if (!isReady()) {
            queryBoxStatus(null);
        }
    }

    public void requestToCheckOut(String compartmentNumber) {
        if (!isReady()) {
            // the locker manager must know all box status to proceed.
            queryBoxStatus(null);
            return;
        }
        if (isBtConnected()) {
            List<String> boxes = Collections.singletonList(compartmentNumber);
            LockerCommand command = new LockerCommand(LockerCommand.COMMAND_TYPE_CHECK_OUT, boxes);
            queueCommand(command);
        }
    }

    public void requestToCharge() {
        if (isBtConnected()) {
            LockerCommand command = new LockerCommand(LockerCommand.COMMAND_TYPE_CHARGE, null);
            queueCommand(command);
        }
    }

    public void requestToDischarge() {
        if (isBtConnected()) {
            LockerCommand command = new LockerCommand(LockerCommand.COMMAND_TYPE_DISCHARGE, null);
            queueCommand(command);
        }
    }

    /**
     * Constant loop and check whether the current command expired by compare the command's sent time and now.
     * If the current command expired, remove it and send the next command in queue to the board.
     * Todo: maybe able to optimize CPU usage by killing this thread when not needed and only let it run when it becomes necessary
     */
    private class CommanderThread extends Thread {
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
