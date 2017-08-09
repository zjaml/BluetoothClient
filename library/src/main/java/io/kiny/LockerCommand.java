package io.kiny;

import java.util.Collection;
import java.util.Date;
import java.util.Locale;

public class LockerCommand {
    private int counter = 0;
    private String _id;
    private LockerCommandType _type;
    private Collection<String> _doors;
    private boolean _allDoor;
    private Date _sent;

    public LockerCommand(LockerCommandType type, Collection<String> doors, boolean allDoor) {
        _id = getIdAndIncrement();
        _type = type;
        _doors = doors;
        _allDoor = allDoor;
    }

    private synchronized String getIdAndIncrement() {
        String id = String.format(Locale.getDefault(), "%02d", counter);
        counter += 1;
        if (counter > 99) {
            counter = 0;
        }
        return id;
    }

    public void recordSent(){
        _sent = new Date();
    }

    @Override
    public String toString() {
        return "";
    }
}
