package io.kiny;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoxStatus {
    private String boxNumber = null;
    private BoxStatusType status;
    public static final String BOX_STATUS_PATTERN = "(\\d{2})(E|F|O)";

    public BoxStatus(String boxStatus){
        Pattern r = Pattern.compile(BOX_STATUS_PATTERN);
        // Now create matcher object.
        Matcher m = r.matcher(boxStatus);
        if (m.find()) {
            boxNumber = m.group(1);
            status = getStatus(m.group(2));
        }
    }

    private BoxStatusType getStatus(String status) {
        switch (status){
            case "E":
                return BoxStatusType.Empty;
            case "F":
                return BoxStatusType.Full;
            case "O":
                return BoxStatusType.Open;
            default:
                return null;
        }
    }

    public String getBoxNumber() {
        return boxNumber;
    }

    public BoxStatusType getStatus() {
        return status;
    }
}
