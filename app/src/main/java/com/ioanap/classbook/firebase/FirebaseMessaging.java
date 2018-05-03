package com.ioanap.classbook.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ioanap.classbook.R;

/**
 * Created by Ioana Pascu on 5/3/2018.
 */

public class FirebaseMessaging extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessaging";
    private static final String CHANNEL_NAME = "Channel";
    private static final String CHANNEL_ID = "channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String notificationTitle = remoteMessage.getNotification().getTitle();
        String notificationMessage = remoteMessage.getNotification().getBody();
        String clickAction = remoteMessage.getNotification().getClickAction();
        String notificationType = remoteMessage.getData().get("notification_type");

        // send notification
        Intent intent = new Intent(clickAction);

        if (notificationType.equals("request")) {
            String fromUserId = remoteMessage.getData().get("from_user_id");
            intent.putExtra("userId", fromUserId);
        } else if (notificationType.equals("event")) {
            String classId = remoteMessage.getData().get("classId");
            intent.putExtra("classId", classId);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* request code */,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long[] pattern = {500, 500, 500, 500, 500};

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_email_envelope)
                .setContentTitle(notificationTitle)
                .setContentText(notificationMessage)
                .setAutoCancel(true)
                .setVibrate(pattern)
                .setLights(Color.BLUE, 1, 1)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel (only on API 26+ because)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            // Sets whether notifications posted to this channel should display notification lights
            channel.enableLights(true);
            // Sets whether notification posted to this channel should vibrate.
            channel.enableVibration(true);
            // Sets the notification light color for notifications posted to this channel
            channel.setLightColor(Color.BLUE);
            // Sets whether notifications posted to this channel appear on the lockscreen or not
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
