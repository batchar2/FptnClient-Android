package org.fptn.vpn.utils;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;

public class NotificationUtils {

    public static void configureNotificationChannel(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        int notificationChannelOnDevice = sharedPreferences.getInt(Constants.MAIN_NOTIFICATION_CHANNEL_VERSION, 0);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(Constants.MAIN_NOTIFICATION_CHANNEL_ID);
        // remove existed notification channel if their version lower than in constants
        if (notificationChannel != null && notificationChannelOnDevice < Constants.MAIN_NOTIFICATION_CHANNEL_VERSION_NUM) {
            notificationManager.deleteNotificationChannel(Constants.MAIN_NOTIFICATION_CHANNEL_ID);
            notificationChannel = null;
        }

        if (notificationChannel == null) {
            notificationManager.createNotificationChannelGroup(
                    new NotificationChannelGroup(Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID, context.getString(R.string.notification_group_name)));

            NotificationChannel newNotificationChannel = new NotificationChannel(
                    Constants.MAIN_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            newNotificationChannel.setGroup(Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID);
            newNotificationChannel.setSound(null, null); //disable sound
            notificationManager.createNotificationChannel(
                    newNotificationChannel
            );

            sharedPreferences.edit().putInt(Constants.MAIN_NOTIFICATION_CHANNEL_VERSION, Constants.MAIN_NOTIFICATION_CHANNEL_VERSION_NUM).apply();
        }
    }
}
