package io.kiny;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by JZhao on 8/9/2017.
 * response
 */

public class LockerResponse {
    public static final String RESPONSE_PATTERN = "(\\d{2}):(\\w)(.*)";

    private String _id;
    private List<BoxStatus> boxStatusList;

    public LockerResponse(String response) {
        // must be in format ID:Type[Door], like '01:A01'
        // Create a Pattern object
        Pattern r = Pattern.compile(RESPONSE_PATTERN);
        // Now create matcher object.
        Matcher m = r.matcher(response);
        if (m.find()) {
            _id = m.group(1);
            String boxStatus = m.group(3);
            if (boxStatus.length() > 0) {
                String[] statusList = boxStatus.split("&");
                boxStatusList = new ArrayList<>();
                for (String status : statusList) {
                    boxStatusList.add(new BoxStatus(status));
                }
            }
        }
    }

    public String getId() {
        return _id;
    }

    public List<BoxStatus> getBoxStatus() {
        return boxStatusList;
    }
}
