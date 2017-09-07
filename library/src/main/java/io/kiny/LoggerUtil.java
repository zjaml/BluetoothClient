package io.kiny;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by JZhao on 9/7/2017.
 */

public class LoggerUtil {
    public static void d(String tag, String message) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.US);
        Log.d(tag, String.format("%s %s", formatter.format((new Date())), message));
    }

    public static void e(String tag, String message) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS", Locale.US);
        Log.e(tag, String.format("%s %s", formatter.format((new Date())), message));
    }
}
