package io.kiny;

import android.text.TextUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LockerCommand {
    private int counter = 0;
    private String _id;
    private LockerCommandType _type;
    private List<String> _doors;
    private boolean _allDoor;
    private int _lifeSpanInSeconds;
    private Date _sent;

    public LockerCommand(LockerCommandType type, List<String> doors, boolean allDoor) {
        _id = getIdAndIncrement();
        _type = type;
        _doors = doors;
        _allDoor = allDoor;
        _lifeSpanInSeconds = 60;
    }

    private synchronized String getIdAndIncrement() {
        String id = String.format(Locale.getDefault(), "%02d", counter);
        counter += 1;
        if (counter > 99) {
            counter = 0;
        }
        return id;
    }

    public void recordSent() {
        _sent = new Date();
    }

    public boolean expired() {
        long now = new Date().getTime();
        long sent = _sent.getTime();
        return sent + _lifeSpanInSeconds * 1000 > now;
    }

    @Override
    public String toString() {
        switch (_type) {
            case CheckIn:
                return String.format("O%2sT", _doors.get(0));
            case CheckOut:
                return String.format("O%2sR", _doors.get(0));
            case DoorStatus:
                return _allDoor ? "D" : String.format("D&%s", TextUtils.join("&", _doors));
            case EmptyStatus:
                return _allDoor ? "E" : String.format("E&%s", TextUtils.join("&", _doors));
            case Charge:
                return "LOW";
            case Discharge:
                return "HIGH";
            default:
                return "";
        }
    }

    public String getId() {
        return _id;
    }

    public Date getSent(){
        return _sent;
    }
}
