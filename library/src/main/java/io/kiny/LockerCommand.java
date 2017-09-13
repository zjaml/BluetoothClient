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
    public static final String COMMAND_TYPE_BUZZER_ON = "Y";
    public static final String COMMAND_TYPE_BUZZER_OFF = "Z";
    public static final int COMMAND_MAX_DURATION = 2000;

    private static int counter = 0;
    private String _id;
    private String _type;
    private List<String> _boxes;
    private int _lifeSpanInSeconds;
    private Date _sent;

    public static final String COMMAND_PATTERN = "(\\d{2}):(\\w)(.*)";

    public LockerCommand(String type, List<String> boxes) {
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
            _type = m.group(2);
            if(m.group(3).length() > 0) {
                String[] boxes = m.group(3).split(BOX_SEPARATOR);
                _boxes = Arrays.asList(boxes);
            }
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
        return sent + _lifeSpanInSeconds * COMMAND_MAX_DURATION < now;
    }

    @Override
    public String toString() {
        return String.format("%s:%s%s", _id, _type, getBoxesString());
    }


    private String getBoxesString() {
        if (_boxes != null) {
            return TextUtils.join(BOX_SEPARATOR, _boxes);
        }
        return "";
    }

    public String getId() {
        return _id;
    }

    public String getType() {
        return _type;
    }

    public List<String> getBoxes() {
        return _boxes;
    }

    public Date getSent() {
        return _sent;
    }
}
