```java
package com.example.activitystreak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PowerManager; // <-- ADD THIS IMPORT

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * {@code ReminderReceiver} is a {@link BroadcastReceiver} responsible for handling broadcast intents
 * (typically triggered by an {@link android.app.AlarmManager}) to display task reminder notifications.
 * <p>
 * This receiver ensures that even if the device is in a low-power state (Doze mode) or the CPU
 * is suspended, the notification can be reliably displayed by acquiring a {@link PowerManager.WakeLock}.
 * It constructs and displays a notification using the provided task details from the incoming {@link Intent}.
 * </p>
 *
 * <p>
 * **Key Responsibilities:**
 * <ul>
 *     <li>Receiving scheduled reminder intents.</li>
 *     <li>Extracting task title and description from the intent extras.</li>
 *     <li>Acquiring a {@link PowerManager.WakeLock} to guarantee notification display, especially
 *         if the device is in a low-power state.</li>
 *     <li>Building and displaying a high-priority notification with the task details.</li>
 *     <li>Releasing the {@link PowerManager.WakeLock} to prevent battery drain.</li>
 *     <li>Checking for {@link android.Manifest.permission#POST_NOTIFICATIONS} before displaying
 *         the notification on Android 13 (API 33) and above.</li>
 * </ul>
 * </p>
 */
public class ReminderReceiver extends BroadcastReceiver {

    /**
     * Called when the BroadcastReceiver is receiving an Intent broadcast.
     * This method is the entry point for handling reminder events. It extracts task
     * information from the intent, acquires a wakelock to ensure the notification is shown,
     * builds and displays the notification, and then safely releases the wakelock.
     *
     * @param context The Context in which the receiver is running. This context is used
     *                to access system services like {@link PowerManager} and
     *                {@link NotificationManagerCompat}.
     * @param intent  The Intent being received. This intent is expected to contain
     *                the task details as extras.
     *                <ul>
     *                    <li>Requires an extra {@code "taskTitle"} (String) for the notification title.</li>
     *                    <li>May contain an extra {@code "taskDesc"} (String) for the notification content text.</li>
     *                </ul>
     *                If {@code intent} is null or {@code "taskTitle"} is missing, the method
     *                will gracefully return without processing.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // --- SAFETY CHECK ---
        // Validate the incoming intent to prevent NullPointerExceptions and ensure
        // meaningful notifications are created. If the intent or its essential data
        // is missing, the receiver will not proceed.
        if (intent == null || intent.getStringExtra("taskTitle") == null) {
            return; // Ignore invalid or malformed intents
        }

        // Extract task details from the intent extras. These values are used
        // to populate the notification.
        String taskTitle = intent.getStringExtra("taskTitle");
        String taskDesc = intent.getStringExtra("taskDesc");
        // Retrieve the notification channel ID from a centralized location (AddPageActivity)
        // to ensure consistency and correct channel assignment.
        String channelId = AddPageActivity.REMINDER_CHANNEL_ID;

        // --- WAKELOCK LOGIC TO ENSURE NOTIFICATION IS SHOWN ---
        // Acquire a PowerManager.WakeLock to keep the CPU running long enough
        // to display the notification, especially if the device is in Doze mode
        // or the CPU is about to be suspended. This prevents the notification
        // from being delayed or missed.
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        // Create a PARTIAL_WAKE_LOCK which keeps the CPU on but allows the screen to go off.
        // A tag "ActivityStreak::ReminderWakeLock" is used for debugging and identification.
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ActivityStreak::ReminderWakeLock");
        // Acquire the wakelock with a timeout of 10 seconds. This ensures the wakelock
        // is eventually released even if something goes wrong, preventing excessive battery drain.
        wakeLock.acquire(10 * 1000L /* 10 seconds timeout */);
        //---------------------------------------------------------

        try {
            // Build the notification using NotificationCompat.Builder for backward compatibility
            // and consistent styling.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.activity_streak) // Set the small icon for the notification (required).
                    .setContentTitle("Task Reminder: " + taskTitle) // Set the main title of the notification.
                    .setContentText(taskDesc) // Set the main content text of the notification.
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // Set high priority to make the notification more prominent (e.g., heads-up notification).
                    .setAutoCancel(true); // Make the notification disappear when the user taps it.

            // Get an instance of NotificationManagerCompat to manage notifications.
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // Generate a unique notification ID. Using System.currentTimeMillis() is a simple
            // way to get a unique ID for each reminder, ensuring multiple reminders can be
            // displayed concurrently without overwriting each other.
            int notificationId = (int) System.currentTimeMillis();

            // Check for POST_NOTIFICATIONS permission (Android 13 / API 33+)
            // Starting from Android 13, apps need explicit permission to post notifications.
            // This check ensures the notification is only shown if permission is granted,
            // preventing crashes or failed notifications on newer Android versions.
            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // If permission is granted (or not required on older APIs), display the notification.
                notificationManager.notify(notificationId, builder.build());
            } else {
                // Optionally, log a warning here if permission is denied,
                // indicating why the notification might not have been shown.
                // Log.w("ReminderReceiver", "Notification permission denied. Cannot show reminder.");
            }
        } finally {
            // --- ALWAYS RELEASE THE WAKELOCK ---
            // It is CRITICAL to release the wakelock in a finally block to ensure it's
            // always released, regardless of whether the notification display was successful
            // or an exception occurred. Failure to release wakelocks leads to severe
            // battery drain.
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            //--------------------------------------
        }
    }
}
```

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
