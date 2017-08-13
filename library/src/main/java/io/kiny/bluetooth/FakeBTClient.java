package io.kiny.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.kiny.BoxStatus;
import io.kiny.LockerCommand;
import io.kiny.LockerResponse;

/**
 * Created by JZhao on 2/20/2017.
 * Locker simulator
 */

public class FakeBTClient implements BluetoothClientInterface {
    private final String Tag = "FakeBTClient";
    private final Handler mHandler;
    private final Boolean mSimulateDisconnection;
    private final SafeBroadcastReceiver mBluetoothBroadcastReceiver;
    private int mState;
    private Date connected;

    private Map<String, Integer> openDoors;


    public FakeBTClient(Handler handler, Boolean simulateDisconnection) {
        mState = STATE_NONE;
        mHandler = handler;
        mSimulateDisconnection = simulateDisconnection;
        mBluetoothBroadcastReceiver = new SafeBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    Log.d(Tag, "bluetooth disconnection detected!");
                    setState(STATE_NONE);
                }
            }
        };
        // simulate door closing by setting open time for each door.
        openDoors = new HashMap<>();
        openDoors.put("05", 30000);
        openDoors.put("10", 20000);
        openDoors.put("29", 60000);
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public SafeBroadcastReceiver getBluetoothBroadcastReceiver() {
        return mBluetoothBroadcastReceiver;
    }

    void setState(int state) {
        if (mState == STATE_CONNECTED && state != STATE_CONNECTED) {
            // if the state was connected and it changed, notify the caller the connect was lost so that
            // the caller may initiate connect again. we don't want to send noise to the caller because connect is an expensive call.
            mHandler.obtainMessage(Constants.MESSAGE_CONNECTION_LOST, state).sendToTarget();
            Log.d(Tag, "BT Connection Lost");
        }
        if (mState != STATE_CONNECTED && state == STATE_CONNECTED) {
            connected = new Date();
            mHandler.obtainMessage(Constants.MESSAGE_CONNECTED, state).sendToTarget();
            Log.d(Tag, "BT Connection established");
        }
        mState = state;
    }

    @Override
    public boolean connect() {
        AsyncTask<Void, Void, Void> delayed = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Thread.sleep(2000);
                    setState(STATE_CONNECTED);
                    // use following to simulate occasional disconnection.
                    //don't wait too long here as it jams the queue, preventing future AsyncTask from running.
                    //executeOnExecutor helps a bit.
                    if (mSimulateDisconnection) {
                        Thread.sleep(10000);
                        setState(STATE_NONE);
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        //seem to run async tasks sequentially.
//        delayed.execute();
        delayed.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        return true;
    }

    @Override
    public void disconnect() {
        setState(STATE_NONE);
    }

    @Override
    public void sendCommand(final String command) {
        AsyncTask<Void, Void, Void> delayed = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Thread.sleep(500);
                    LockerCommand currentCommand = new LockerCommand(command);
                    switch (currentCommand.getType()) {
                        case LockerCommand.COMMAND_TYPE_CHECK_IN:
                        case LockerCommand.COMMAND_TYPE_CHECK_OUT: {
                            List<BoxStatus> boxStatusList = new ArrayList<>();
                            boxStatusList.add(new BoxStatus(currentCommand.getBoxes().get(0), BoxStatus.BOX_OPEN));
                            LockerResponse response = new LockerResponse(currentCommand.getId(), LockerResponse.RESPONSE_TYPE_BOX_STATUS, boxStatusList);
                            mHandler.obtainMessage(Constants.MESSAGE_INCOMING_MESSAGE, response.toString()).sendToTarget();
                            break;
                        }
                        case LockerCommand.COMMAND_TYPE_BOX_STATUS: {
                            List<String> boxes = currentCommand.getBoxes();
                            if (boxes == null) {
                                boxes = new ArrayList<>();
                                for (int i = 0; i < 30; i++) {
                                    boxes.add(String.format(Locale.US, "%02d", i));
                                }
                            }
                            List<BoxStatus> boxStatusList = new ArrayList<>();
                            for (String box : boxes) {
                                String status;
                                if (openDoors.containsKey(box)) {
                                    int timeToClose = openDoors.get(box);
                                    if (timeToClose + connected.getTime() < (new Date()).getTime()) {
                                        status = BoxStatus.BOX_FULL;
                                    } else {
                                        status = BoxStatus.BOX_OPEN;
                                    }
                                } else if (Integer.parseInt(box) % 3 == 0) {
                                    status = BoxStatus.BOX_EMPTY;
                                } else {
                                    status = BoxStatus.BOX_FULL;
                                }
                                boxStatusList.add(new BoxStatus(box, status));
                            }
                            LockerResponse response = new LockerResponse(currentCommand.getId(), LockerResponse.RESPONSE_TYPE_BOX_STATUS, boxStatusList);
                            mHandler.obtainMessage(Constants.MESSAGE_INCOMING_MESSAGE, response.toString()).sendToTarget();
                            break;
                        }
                        case LockerCommand.COMMAND_TYPE_CHARGE: {
                            LockerResponse response = new LockerResponse(currentCommand.getId(), LockerResponse.RESPONSE_TYPE_CHARGING, null);
                            mHandler.obtainMessage(Constants.MESSAGE_INCOMING_MESSAGE, response.toString()).sendToTarget();
                            break;
                        }
                        case LockerCommand.COMMAND_TYPE_DISCHARGE: {
                            LockerResponse response = new LockerResponse(currentCommand.getId(), LockerResponse.RESPONSE_TYPE_DISCHARGING, null);
                            mHandler.obtainMessage(Constants.MESSAGE_INCOMING_MESSAGE, response.toString()).sendToTarget();
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        delayed.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }
}
