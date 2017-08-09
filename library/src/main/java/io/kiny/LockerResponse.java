package io.kiny;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by JZhao on 8/9/2017.
 * response
 */

public class LockerResponse {
    public static final String RESPONSE_PATTERN = "(\\d{2}):(\\w)(.*)";

    private String _id;

    public LockerResponse(String response) {
        // must be in format ID:Type[Door], like '01:A01'
        // Create a Pattern object
        Pattern r = Pattern.compile(RESPONSE_PATTERN);

        // Now create matcher object.
        Matcher m = r.matcher(response);
        if (m.find()) {
            _id = m.group(1);
        }
    }

    public String getId() {
        return _id;
    }
}
