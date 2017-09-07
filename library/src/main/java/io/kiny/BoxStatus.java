package io.kiny;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoxStatus {

    public static final String BOX_EMPTY = "E";
    public static final String BOX_FULL = "F";
    public static final String BOX_OPEN = "O";
    private String _boxNumber = null;
    private String _status;
    public static final String BOX_STATUS_PATTERN = "(\\d{2})(E|F|O)";

    public BoxStatus(String boxStatus) throws InvalidLockerResponseException {
        Pattern r = Pattern.compile(BOX_STATUS_PATTERN);
        // Now create matcher object.
        Matcher m = r.matcher(boxStatus);
        if (m.find()) {
            _boxNumber = m.group(1);
            _status = m.group(2);
        }else{
            throw new InvalidLockerResponseException();
        }
    }

    public BoxStatus(String boxNumber, String status) {
        _boxNumber = boxNumber;
        _status = status;
    }

    public String getBoxNumber() {
        return _boxNumber;
    }

    public String getStatus(){
        return _status;
    }

    @Override
    public String toString() {
        return String.format("%2s%s", _boxNumber, _status);
    }
}
