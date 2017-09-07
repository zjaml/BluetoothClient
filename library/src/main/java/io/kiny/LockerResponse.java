package io.kiny;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by JZhao on 8/9/2017.
 * response
 */

public class LockerResponse {
    public static final String BOX_SEPARATOR = "&";
    public static final String RESPONSE_TYPE_NEGATIVE = "N";
    public static final String RESPONSE_TYPE_BOX_STATUS = "B";
    public static final String RESPONSE_TYPE_CHARGING = "C";
    public static final String RESPONSE_TYPE_DISCHARGING = "D";

    public static final String RESPONSE_PATTERN = "(\\d{2}):(\\w)(.*)";

    private String _id;
    private String _type;
    private List<BoxStatus> _boxStatus;

    public LockerResponse(String response) throws InvalidLockerResponseException {
        Pattern r = Pattern.compile(RESPONSE_PATTERN);
        // Now create matcher object.
        Matcher m = r.matcher(response);
        if (m.find()) {
            _id = m.group(1);
            _type = m.group(2);
            String boxStatus = m.group(3);
            if (boxStatus.length() > 0) {
                String[] statusList = boxStatus.split("&");
                _boxStatus = new ArrayList<>();
                for (String status : statusList) {
                    _boxStatus.add(new BoxStatus(status));
                }
            }
        }
    }

    public LockerResponse(String id, String type, List<BoxStatus> boxStatus) {
        _id = id;
        _type = type;
        _boxStatus = boxStatus;
    }

    public String getId() {
        return _id;
    }

    public String getType() {
        return _type;
    }

    public List<BoxStatus> getBoxStatus() {
        return _boxStatus;
    }

    @Override
    public String toString() {
        return String.format("%s:%s%s", _id, _type, boxStatusString());
    }

    private String boxStatusString() {
        if (_boxStatus != null) {
            return TextUtils.join(BOX_SEPARATOR, _boxStatus);
        }
        return "";
    }
}
