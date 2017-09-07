package io.kiny;

/**
 * Created by JZhao on 8/18/2017.
 */

public interface LockerCallback {
    void ready();

    void connected();

    void disconnected();

    void charging();

    void discharging();

    void boxOpened(String box);

    void boxClosed(String box, String status);

    void onException(String errorMessage);
}
