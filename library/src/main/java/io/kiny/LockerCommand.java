package io.kiny;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LockerCommand {
    public static final String BOX_SEPARATOR = "&";
    public static final String COMMAND_TYPE_CHECK_IN = "T";
    public static final String COMMAND_TYPE_CHECK_OUT = "R";
    public static final String COMMAND_TYPE_BOX_STATUS = "B";
    public static final String COMMAND_TYPE_DISCHARGE = "H";
    public static final String COMMAND_TYPE_CHARGE = "L";
    private int counter = 0;
    private String _id;
    private LockerCommandType _type;
    private List<String> _boxes;
    private int _lifeSpanInSeconds;
    private Date _sent;

    public static final String COMMAND_PATTERN = "(\\d{2}):(\\w)(.*)";

    public LockerCommand(LockerCommandType type, List<String> boxes) {
        _id = getIdAndIncrement();
        _type = type;
        _boxes = boxes;
        _lifeSpanInSeconds = 60;
    }

    public LockerCommand(String command) {
        Pattern r = Pattern.compile(COMMAND_PATTERN);
        // Now create matcher object.
        Matcher m = r.matcher(command);
        if (m.find()) {
            _id = m.group(1);
            _type = getType(m.group(2));
            String[] boxes = m.group(3).split(BOX_SEPARATOR);
            _boxes = Arrays.asList(boxes);
        }
    }

    private LockerCommandType getType(String type) {
        switch (type) {
            case COMMAND_TYPE_CHECK_IN:
                return LockerCommandType.CheckIn;
            case COMMAND_TYPE_CHECK_OUT:
                return LockerCommandType.CheckOut;
            case COMMAND_TYPE_BOX_STATUS:
                return LockerCommandType.BoxStatus;
            case COMMAND_TYPE_DISCHARGE:
                return LockerCommandType.Discharge;
            case COMMAND_TYPE_CHARGE:
                return LockerCommandType.Charge;
            default:
                return null;
        }
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
            case CheckOut:
                return String.format("%s:%s%2s", _id, _type, _boxes.get(0));
            case BoxStatus:
                return String.format("%s:%s%s", _id, _type, getBoxes());
            case Charge:
            case Discharge:
                return String.format("%s:%s", _id, _type);
            default:
                return "";
        }
    }

    private String getBoxes() {
        if (_boxes != null) {
            return TextUtils.join(BOX_SEPARATOR, _boxes);
        }
        return "";
    }

    public String getId() {
        return _id;
    }

    public Date getSent() {
        return _sent;
    }
}
