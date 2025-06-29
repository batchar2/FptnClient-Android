package org.fptn.vpn.utils;

import java.util.Locale;

public class TimeUtils {

    public static String getTime(long durationInSeconds) {
        long hours = durationInSeconds / 3600;
        long minutes = (durationInSeconds % 3600) / 60;
        long seconds = durationInSeconds % 60;

        // Format and display the time as HH:MM:SS
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }
}
