package io.kiny.example;
import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import io.kiny.LockerManager;

public class MainActivity extends AppCompatActivity {
    @SuppressLint("StaticFieldLeak")
    public static LockerManager mLockerManager;
    Button checkinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockerManager = new LockerManager("HC06", getApplicationContext(), true);
        setContentView(R.layout.activity_main);
        checkinButton = (Button)findViewById(R.id.checkinButton);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLockerManager.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLockerManager.stop();
    }

    public void onCheckInClicked(View view) {
        mLockerManager.requestToCheckIn(10);
    }
}
