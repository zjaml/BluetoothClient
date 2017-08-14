package io.kiny.bluetooth;

/**
 * Created by JZhao on 8/14/2017.
 */

public interface BluetoothCallback {
    void onBluetoothEvent(int eventType, String message);
}
