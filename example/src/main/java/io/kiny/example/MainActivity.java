package io.kiny.example;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.Objects;

import io.kiny.LockerManager;

public class MainActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    public LockerManager mLockerManager;
    TextView logtxt;

    private BroadcastReceiver onNotice = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), LockerManager.ACTION_LOCKER_BOX_OPENED)) {
                String log = String.format("%s Box Number:%s \n",
                        intent.getAction(),
                        intent.getStringExtra("box"));
                logtxt.append(log);
            } else if (Objects.equals(intent.getAction(), LockerManager.ACTION_LOCKER_BOX_CLOSED)) {
                String log = String.format("%s Box Number: %s Has item: %s\n",
                        intent.getAction(),
                        intent.getStringExtra("box"),
                        intent.getBooleanExtra("isEmpty", false));
                logtxt.append(log);
            } else {
                logtxt.append(intent.getAction() + "\n");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockerManager = new LockerManager("HC-06", getApplicationContext(), true);
        mLockerManager.start();
        setContentView(R.layout.activity_main);
        logtxt = (TextView) findViewById(R.id.logTxt);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLockerManager.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LockerManager.ACTION_LOCKER_BOX_CLOSED);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_BOX_OPENED);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_CHARGING);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_DISCHARGING);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_CONNECTED);
        intentFilter.addAction(LockerManager.ACTION_LOCKER_DISCONNECTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, intentFilter);
    }

    public void onCheckInClicked(View view) {
        mLockerManager.requestToCheckIn("10");
    }
}
