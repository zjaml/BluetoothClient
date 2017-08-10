package io.kiny.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.kiny.BoxStatus;
import io.kiny.LockerCommand;
import io.kiny.LockerResponse;

/**
 * Created by JZhao on 2/20/2017.
 *
 */

public class FakeBTClient implements BluetoothClientInterface {
    private final String Tag = "FakeBTClient";
    private final Handler mHandler;
    private final Boolean mSimulateDisconnection;
    private final SafeBroadcastReceiver mBluetoothBroadcastReceiver;
    private int mState;

    public FakeBTClient(Handler handler, Boolean simulateDisconnection) {
        mState = STATE_NONE;
        mHandler = handler;
        mSimulateDisconnection = simulateDisconnection;
        mBluetoothBroadcastReceiver = new SafeBroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                    Log.d(Tag, "bluetooth disconnection detected!");
                    // todo: crash report.
                    setState(STATE_NONE);
                }
            }
        };
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
                            LockerResponse response = new LockerResponse(currentCommand.getId(), LockerResponse.RESPONSE_TYPE_BOX_OPEN, null);
                            mHandler.obtainMessage(Constants.MESSAGE_INCOMING_MESSAGE, response.toString()).sendToTarget();
                            break;
                        }
                        case LockerCommand.COMMAND_TYPE_BOX_STATUS: {
                            if (currentCommand.hasBoxes()) {
                                List<BoxStatus> boxStatusList = new ArrayList<>();
                                List<String> boxes = currentCommand.getBoxes();
                                for (String box : boxes) {
                                    boxStatusList.add(new BoxStatus(box, BoxStatus.BOX_OPEN));
                                }
                                LockerResponse response = new LockerResponse(currentCommand.getId(), LockerResponse.RESPONSE_TYPE_BOX_STATUS, boxStatusList);
                                mHandler.obtainMessage(Constants.MESSAGE_INCOMING_MESSAGE, response.toString()).sendToTarget();
                            } else {
                                List<BoxStatus> boxStatusList = new ArrayList<>();
                                for (int i = 0; i < 30; i++) {
                                    // set 05 29 as open
                                    String status;
                                    if (i % 3 == 0) {
                                        status = BoxStatus.BOX_EMPTY;
                                    } else if (i == 5 || i == 29) {
                                        status = BoxStatus.BOX_OPEN;
                                    } else {
                                        status = BoxStatus.BOX_FULL;
                                    }
                                    boxStatusList.add(new BoxStatus(String.format(Locale.US, "%02d", i), status));
                                }
                                LockerResponse response = new LockerResponse(currentCommand.getId(), LockerResponse.RESPONSE_TYPE_BOX_STATUS, boxStatusList);
                                mHandler.obtainMessage(Constants.MESSAGE_INCOMING_MESSAGE, response.toString()).sendToTarget();
                            }
                            break;
                        }
                        case LockerCommand.COMMAND_TYPE_CHARGE: {
                            LockerResponse response = new LockerResponse(currentCommand.getId(), LockerResponse.RESPONSE_TYPE_CHARGEING, null);
                            mHandler.obtainMessage(Constants.MESSAGE_INCOMING_MESSAGE, response.toString()).sendToTarget();
                            break;
                        }
                        case LockerCommand.COMMAND_TYPE_DISCHARGE: {
                            LockerResponse response = new LockerResponse(currentCommand.getId(), LockerResponse.RESPONSE_TYPE_DISCHARGEING, null);
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
