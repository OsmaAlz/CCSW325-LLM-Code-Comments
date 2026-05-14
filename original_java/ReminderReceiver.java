package com.example.activitystreak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PowerManager; // <-- ADD THIS IMPORT

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // --- SAFETY CHECK ---
        if (intent == null || intent.getStringExtra("taskTitle") == null) {
            return; // Ignore invalid intents
        }

        String taskTitle = intent.getStringExtra("taskTitle");
        String taskDesc = intent.getStringExtra("taskDesc");
        String channelId = AddPageActivity.REMINDER_CHANNEL_ID;

        // --- WAKELOCK LOGIC TO ENSURE NOTIFICATION IS SHOWN ---
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivityStreak::ReminderWakeLock");
        wakeLock.acquire(10 * 1000L /* 10 seconds timeout */);
        //---------------------------------------------------------

        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.activity_streak) // Your app icon
                    .setContentTitle("Task Reminder: " + taskTitle)
                    .setContentText(taskDesc)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            int notificationId = (int) System.currentTimeMillis();

            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(notificationId, builder.build());
            }
        } finally {
            // --- ALWAYS RELEASE THE WAKELOCK ---
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            //--------------------------------------
        }
    }
}
