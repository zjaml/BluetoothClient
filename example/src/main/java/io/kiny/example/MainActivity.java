package io.kiny.example;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.wefika.flowlayout.FlowLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.kiny.BoxStatus;
import io.kiny.LockerManager;

public class MainActivity extends AppCompatActivity {
    public static final String TARGET_DEVICE_NAME = "HC-06";
    private LockerManager mLockerManager;
    FlowLayout flowLayout;
    private boolean connected = false;
    private boolean charging = false;
    private List<ToggleButton> boxButtons;

    private BroadcastReceiver lockerBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case LockerManager.ACTION_LOCKER_READY:
                    Log.d("LockerManager", "ACTION_LOCKER_READY");
                    Collection<BoxStatus> allBoxStatus = mLockerManager.getBoxStatus();
                    for (BoxStatus boxstatus : allBoxStatus) {
                        updateBoxStatus(boxstatus.getBoxNumber(), boxstatus.getStatus());
                    }
                case LockerManager.ACTION_LOCKER_CONNECTED:
                    connected = true;
                    setTitle(String.format("%s %s", "Connected",
                            charging ? "Charging" : "Discharging"));
                    break;
                case LockerManager.ACTION_LOCKER_DISCONNECTED:
                    connected = false;
                    setTitle(String.format("%s %s",
                            "Disconnected",
                            charging ? "Charging" : "Discharging"));
                    break;
                case LockerManager.ACTION_LOCKER_CHARGING:
                    showToast("Charging response received");
                    break;
                case LockerManager.ACTION_LOCKER_DISCHARGING:
                    showToast("Discharging response received");
                    break;
                case LockerManager.ACTION_LOCKER_BOX_OPENED: {
                    String boxNumber = intent.getStringExtra("box");
                    String log = String.format("%s Box Number:%s \n",
                            "Opened", boxNumber);
                    updateBoxStatus(boxNumber, "O");
                    break;
                }
                case LockerManager.ACTION_LOCKER_BOX_CLOSED: {
                    String boxNumber = intent.getStringExtra("box");
                    String boxStatus = intent.getStringExtra("status");
                    String log = String.format("%s Box: %s Status: %s\n",
                            "Closed", boxNumber, boxStatus);
                    updateBoxStatus(boxNumber, boxStatus);
                    break;
                }
            }
        }
    };

    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Intent.ACTION_BATTERY_CHANGED:
                case Intent.ACTION_BATTERY_LOW:
                case Intent.ACTION_POWER_CONNECTED:
                case Intent.ACTION_POWER_DISCONNECTED:
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    charging = status == BatteryManager.BATTERY_STATUS_CHARGING;
                    setTitle(String.format("%s %s",
                            connected ? "Connected" : "Disconnected",
                            charging ? "Charging" : "Discharging"));
//                            status == BatteryManager.BATTERY_STATUS_FULL;
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    float batteryPct = level / (float) scale;
                    Log.d("Battery", String.format("Battery: %.0f%%", batteryPct * 100));
                    if (batteryPct < 0.5 && !charging) {
                        mLockerManager.requestToCharge();
                    } else if (batteryPct > 0.55 && charging) {
                        mLockerManager.requestToDischarge();
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockerManager = new LockerManager(TARGET_DEVICE_NAME, getApplicationContext(), true);
        mLockerManager.start();
        setContentView(R.layout.activity_main);
        flowLayout = (FlowLayout) findViewById(R.id.flowLayout);
        initBoxes();
    }

    private void initBoxes() {
        boxButtons = new ArrayList<>();

        for (int i = 1; i <= 30; i++) {
            final MyToggleButton boxButton = new MyToggleButton(this);
            FlowLayout.LayoutParams layoutParams = new FlowLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            final String boxId = String.format(Locale.US, "%02d", i);
            boxButton.setText(String.format("%s E", boxId));
            boxButton.setLayoutParams(layoutParams);
            boxButton.setTag("E");
            boxButton.setMinimumWidth(0);
            boxButton.setMinWidth(0);

            boxButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View box) {
                    final CharSequence[] items = {"CHECK IN", "CHECK OUT"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(String.format("Box: %s", boxId));
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            if (item == 0) {
                                mLockerManager.requestToCheckIn(boxId);
                            } else {
                                mLockerManager.requestToCheckOut(boxId);
                            }
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });
            boxButtons.add(boxButton);
            flowLayout.addView(boxButton);
        }
    }

    private void updateBoxStatus(String strBoxNumber, String boxStatus) {
        int boxNumber = Integer.parseInt(strBoxNumber);
        ToggleButton boxButton = boxButtons.get(boxNumber - 1);
        String title = String.format("%s %s", strBoxNumber, boxStatus);
        boxButton.setTag(boxStatus);
        boxButton.setText(title);
        boxButton.setTextOff(title);
        boxButton.setTextOn(title);
        boxButton.setChecked(Objects.equals(boxStatus, BoxStatus.BOX_OPEN));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLockerManager.stop();
        mLockerManager = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mLockerManager != null && (mLockerManager.getBoxStatus() != null)) {
            Collection<BoxStatus> allBoxStatus = mLockerManager.getBoxStatus();
            for (BoxStatus boxstatus : allBoxStatus) {
                updateBoxStatus(boxstatus.getBoxNumber(), boxstatus.getStatus());
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LockerManager.ACTION_LOCKER_READY);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_BOX_CLOSED);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_BOX_OPENED);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_CHARGING);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_DISCHARGING);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_CONNECTED);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_DISCONNECTED);

        LocalBroadcastManager.getInstance(this).registerReceiver(lockerBroadcastReceiver, intentFilter);

        IntentFilter batteryIntentFilter = new IntentFilter();
        //monitor battery/charging change
        batteryIntentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        batteryIntentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        batteryIntentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        this.registerReceiver(batteryReceiver, batteryIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(batteryReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(lockerBroadcastReceiver);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
