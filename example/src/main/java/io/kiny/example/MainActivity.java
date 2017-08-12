package io.kiny.example;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import io.kiny.LockerManager;

public class MainActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    public LockerManager mLockerManager;
    TextView logtxt;

    private BroadcastReceiver onNotice= new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // intent can contain anydata
            logtxt.append(intent.getAction());

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
        IntentFilter iff= new IntentFilter("connected");
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, iff);
    }

    public void onCheckInClicked(View view) {
        mLockerManager.requestToCheckIn("10");
    }
}
