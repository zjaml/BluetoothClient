package io.kiny.example;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.wefika.flowlayout.FlowLayout;

import java.util.Locale;

import io.kiny.LockerManager;

public class MainActivity extends AppCompatActivity {
    private LockerManager mLockerManager;
    TextView logtxt;
    FlowLayout flowLayout;
    private boolean connected = false;
    private boolean charging = false;

    private BroadcastReceiver onNotice = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case LockerManager.ACTION_LOCKER_CONNECTED:
                    connected = true;
                    break;
                case LockerManager.ACTION_LOCKER_DISCONNECTED:
                    connected = false;
                    break;
                case LockerManager.ACTION_LOCKER_CHARGING:
                    charging = true;
                    break;
                case LockerManager.ACTION_LOCKER_DISCHARGING:
                    charging = false;
                    break;
                case LockerManager.ACTION_LOCKER_BOX_OPENED: {
                    String log = String.format("%s Box Number:%s \n",
                            intent.getAction(),
                            intent.getStringExtra("box"));
                    logtxt.append(log);
                    break;
                }
                case LockerManager.ACTION_LOCKER_BOX_CLOSED: {
                    String log = String.format("%s Box: %s Has item: %s\n",
                            intent.getAction(),
                            intent.getStringExtra("box"),
                            intent.getBooleanExtra("isEmpty", false));
                    logtxt.append(log);
                    break;
                }
            }
            setTitle(String.format("%s %s",
                    connected ? "Connected" : "Disconnected",
                    charging ? "Charging" : "Discharging"));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockerManager = new LockerManager("HC-06", getApplicationContext(), true);
        mLockerManager.start();
        setContentView(R.layout.activity_main);
        flowLayout = (FlowLayout) findViewById(R.id.flowLayout);
        initBoxes();
        logtxt = (TextView) findViewById(R.id.logtxt);
    }

    private void initBoxes() {
        for (int i = 1; i <= 30; i++) {
            MyToggleButton boxButton = new MyToggleButton(this);
            FlowLayout.LayoutParams layoutParams = new FlowLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            String boxId = String.format(Locale.US, "%02d", i);
            String title = boxId;
            boxButton.setText(title);
            boxButton.setTag(boxId);
            boxButton.setLayoutParams(layoutParams);
            boxButton.setMinimumWidth(0);
            boxButton.setMinWidth(0);

            boxButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View box) {
                    ((ToggleButton) box).setChecked(true);
                    final CharSequence[] items = {"CHECK IN", "CHECK OUT"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            if(item==0){
                                LockerManager.requestToCheckIn((String)box.getTag());
                            }
//                            Toast.makeText(MainActivity.this, String.format(Locale.US, "%02d", box.getId()), Toast.LENGTH_SHORT).show();
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });
            flowLayout.addView(boxButton);
        }
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

}
