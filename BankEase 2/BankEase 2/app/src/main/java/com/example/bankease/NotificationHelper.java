package com.example.bankease;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Random;

public class NotificationHelper {

    private static final String CHANNEL_ID = "bankease_channel";
    private static final String CHANNEL_NAME = "BankEase Alerts";
    private static final String CHANNEL_DESC = "Notifications for banking activities";
    private static final String TAG = "NotificationHelper";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "✅ Notification channel created.");
            } else {
                Log.e(TAG, "❌ NotificationManager is null. Channel creation failed.");
            }
        }
    }

    public static void sendNotification(Context context, String title, String message) {
        try {
            Log.d(TAG, "📨 Preparing to send → Title: " + title + ", Message: " + message);

            int icon = R.drawable.ic_notifications;
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(icon)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            int notificationId = new Random().nextInt(999999);

            if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                manager.notify(notificationId, builder.build());
                Log.d(TAG, "✅ Notification dispatched with ID: " + notificationId);
            } else {
                Log.w(TAG, "❌ Notifications are disabled for this app");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification: " + e.getMessage());
        }
    }
}