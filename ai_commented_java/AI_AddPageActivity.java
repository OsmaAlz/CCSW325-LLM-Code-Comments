```java
package com.example.activitystreak;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * AddPageActivity is an Android Activity responsible for allowing users to add new tasks
 * to their activity streak tracker. It provides UI elements for entering task details
 * such as name, description, deadline date, deadline time, and reminder preferences.
 *
 * <p>Key functionalities include:</p>
 * <ul>
 *     <li>Displaying and allowing selection of task deadline dates and times using {@link DatePickerDialog}
 *         and {@link TimePickerDialog}.</li>
 *     <li>Handling user input validation for task details.</li>
 *     <li>Saving new tasks to a Firebase Realtime Database, associated with the currently logged-in user.</li>
 *     <li>Scheduling local notifications as reminders for tasks using {@link AlarmManager},
 *         considering Android 12+ exact alarm scheduling requirements and requesting necessary permissions.</li>
 *     <li>Managing notification channels for task reminders (Android O+).</li>
 *     <li>Requesting POST_NOTIFICATIONS permission for Android 13+ to display notifications.</li>
 * </ul>
 *
 * <p>The activity relies on {@link FirebaseAuth} for user authentication and
 * {@link FirebaseDatabase} for data persistence.</p>
 *
 * @author Your Name/Team (replace with actual author if known)
 * @version 1.0
 * @since 2023-10-26
 * @see ReminderReceiver
 * @see SelectedDateManager
 */
public class AddPageActivity extends AppCompatActivity {

    /**
     * Unique identifier for the notification channel used for task reminders.
     * This ID is crucial for creating and managing the channel on Android O (API 26) and above.
     */
    public static final String REMINDER_CHANNEL_ID = "TASK_REMINDER_CHANNEL";
    /**
     * User-visible name for the notification channel. This name appears in the
     * device's notification settings, allowing users to identify and manage
     * reminders from this app.
     */
    public static final String REMINDER_CHANNEL_NAME = "Task Reminders";

    /**
     * EditText field for the user to enter the task's name.
     */
    EditText taskName;
    /**
     * EditText field for the user to enter a detailed description of the task.
     */
    EditText taskDescription;
    /**
     * EditText field to display and allow selection of the task's deadline date.
     * Tapping this field opens a {@link DatePickerDialog}.
     */
    EditText deadlineDate;
    /**
     * EditText field to display and allow selection of the task's deadline time.
     * Tapping this field opens a {@link TimePickerDialog}.
     */
    EditText deadlineTime;
    /**
     * Button to trigger the action of adding the task to the database and scheduling reminders.
     */
    Button addTaskBtn;
    /**
     * Spinner widget to select the desired reminder offset (e.g., 1 hour before, 1 day before).
     */
    Spinner remainderSpinner;

    /**
     * {@link ActivityResultLauncher} used to request runtime permissions, specifically
     * the {@code android.permission.POST_NOTIFICATIONS} permission for Android 13 (API 33) and above.
     * It handles the permission request flow and provides a callback for the result.
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications will not be shown as permission is denied.", Toast.LENGTH_LONG).show();
                }
            });

    /**
     * Called when the activity is first created.
     * This is where most initialization should go: calling {@code setContentView(int)}
     * to inflate the activity's UI, finding views, setting up listeners,
     * initializing notification components, and handling initial data display.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.
     *                           Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_page); // Inflates the layout 'add_page.xml'

        // Initialize UI elements by finding them from the layout
        taskName = findViewById(R.id.taskName);
        taskDescription = findViewById(R.id.taskDescription);
        deadlineDate = findViewById(R.id.deadlineDate);
        deadlineTime = findViewById(R.id.deadlineTime);
        addTaskBtn = findViewById(R.id.addTaskBtn);
        remainderSpinner = findViewById(R.id.remainderSpinner);

        // Create the notification channel (required for Android O and above)
        createNotificationChannel();
        // Request the POST_NOTIFICATIONS permission (required for Android 13 and above)
        askNotificationPermission();

        // Retrieve the selected date from SelectedDateManager, if available.
        // If no date is selected, default to the current date.
        String selectedDate = SelectedDateManager.getSelectedDate();
        if (selectedDate == null) {
            Toast.makeText(this, "No date selected, defaulting to today.", Toast.LENGTH_SHORT).show();
            Calendar cal = Calendar.getInstance();
            // Format the current date as YYYY-MM-DD
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        }
        deadlineDate.setText(selectedDate); // Set the retrieved or default date to the EditText

        // Set up click listeners for date and time pickers
        setupDatePicker();
        setupTimePicker();
        // Set the click listener for the "Add Task" button
        addTaskBtn.setOnClickListener(this::addTaskButtonListener);
    }

    /**
     * Sets up an {@link android.view.View.OnClickListener} for the {@link #deadlineDate} EditText.
     * When the EditText is clicked, a {@link DatePickerDialog} is displayed, allowing the user
     * to select a new date. The selected date is then formatted as "YYYY-MM-DD" and set
     * back to the {@link #deadlineDate} EditText.
     */
    private void setupDatePicker() {
        deadlineDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance(); // Get current date to pre-fill the picker
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddPageActivity.this, // Context for the dialog
                    (view, y, m, d) -> { // Listener for when a date is set
                        // Format month to be 1-indexed and pad with leading zero if needed
                        String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
                        deadlineDate.setText(date); // Update the EditText with the selected date
                    },
                    year, month, day // Initial date for the picker
            );
            datePickerDialog.show(); // Display the date picker dialog
        });
    }

    /**
     * Sets up an {@link android.view.View.OnClickListener} for the {@link #deadlineTime} EditText.
     * When the EditText is clicked, a {@link TimePickerDialog} is displayed, allowing the user
     * to select a new time. The selected time is then formatted as "HH:MM" (24-hour format)
     * and set back to the {@link #deadlineTime} EditText.
     */
    private void setupTimePicker() {
        deadlineTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance(); // Get current time to pre-fill the picker
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    AddPageActivity.this, // Context for the dialog
                    (view, h, m) -> { // Listener for when a time is set
                        // Format hour and minute to pad with leading zeros if needed
                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                        deadlineTime.setText(formattedTime); // Update the EditText with the selected time
                    },
                    hour, minute, true // Initial time for the picker, 'true' for 24-hour format
            );
            timePickerDialog.show(); // Display the time picker dialog
        });
    }

    /**
     * Calculates and returns the reminder offset time in milliseconds based on the
     * currently selected item in the {@link #remainderSpinner}.
     *
     * <p>The method maps spinner positions to predefined time durations:</p>
     * <ul>
     *     <li>Position 0: 15 seconds (for testing purposes)</li>
     *     <li>Position 1: 1 hour</li>
     *     <li>Position 2: 2 hours</li>
     *     <li>Position 3: 1 day</li>
     *     <li>Position 4: 2 days</li>
     *     <li>Position 5: 1 week</li>
     *     <li>Default: 0 (no reminder)</li>
     * </ul>
     * <p>Note: Using 'L' ensures that the numeric literals are treated as 'long' to prevent
     * potential integer overflow during calculation of large time durations.</p>
     *
     * @return The reminder offset in milliseconds, or 0 if no reminder is selected or for an unknown position.
     */
    public long getReminderOffsetMillis() {
        // Returns the selected reminder offset time in milliseconds
        // Using 'L' ensures the numbers are treated as 'long' to prevent overflow
        switch (remainderSpinner.getSelectedItemPosition()) {
            case 0: return 15 * 1000L;               // 15 SECONDS FOR TESTING purposes
            case 1: return 60 * 60 * 1000L;          // 1 hour
            case 2: return 2 * 60 * 60 * 1000L;      // 2 hours
            case 3: return 24 * 60 * 60 * 1000L;     // 1 day
            case 4: return 2 * 24 * 60 * 60 * 1000L; // 2 days
            case 5: return 7 * 24 * 60 * 60 * 1000L; // 1 week
            default: return 0; // No reminder, or an invalid selection
        }
    }

    /**
     * This method is called when the "Add Task" button ({@link #addTaskBtn}) is clicked.
     * It performs the following actions:
     * <ol>
     *     <li>Retrieves task details from {@link #taskName}, {@link #taskDescription},
     *         {@link #deadlineDate}, {@link #deadlineTime}, and {@link #remainderSpinner}.</li>
     *     <li>Validates that essential fields (name, date, time) are not empty.</li>
     *     <li>Checks if a user is currently logged in via Firebase.</li>
     *     <li>If a reminder offset is selected (i.e., greater than 0), it calculates the
     *         exact time for the reminder (current time + offset) and schedules it
     *         using {@link #scheduleReminder(long, String, String)}.</li>
     *     <li>Constructs a map of task data including a generated unique {@code taskId}.</li>
     *     <li>Saves the task data to the Firebase Realtime Database under
     *         {@code users/{uid}/tasks/{dateStr}/{taskId}}.</li>
     *     <li>Displays a {@link Toast} message indicating success or failure of the task addition.</li>
     *     <li>If successful, finishes the activity, returning to the previous screen.</li>
     * </ol>
     *
     * @param view The view that was clicked (the "Add Task" button).
     */
    public void addTaskButtonListener(View view) {
        // Retrieve and trim input from UI fields
        String name = taskName.getText().toString().trim();
        String desc = taskDescription.getText().toString().trim();
        String dateStr = deadlineDate.getText().toString().trim();
        String timeStr = deadlineTime.getText().toString().trim();
        long reminderOffset = getReminderOffsetMillis(); // Get the selected reminder offset

        // Input validation
        if (name.isEmpty() || dateStr.isEmpty() || timeStr.isEmpty()) {
            Toast.makeText(this, "Task name, date, and time are required", Toast.LENGTH_SHORT).show();
            return; // Stop further execution if validation fails
        }

        // Check for logged-in Firebase user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to add a task.", Toast.LENGTH_SHORT).show();
            return; // Stop further execution if no user is logged in
        }

        // --- Simplified Reminder Logic ---
        // Schedules a notification for a duration FROM NOW, based on the reminderOffset.
        if (reminderOffset > 0) {
            long reminderTimeMillis = System.currentTimeMillis() + reminderOffset;
            scheduleReminder(reminderTimeMillis, name, desc);
        }

        // --- Save to Firebase ---
        String uid = currentUser.getUid(); // Get the current user's unique ID
        String taskId = "task_" + System.currentTimeMillis(); // Generate a unique task ID

        // Prepare task data as a HashMap
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("taskId", taskId);
        taskMap.put("taskName", name);
        taskMap.put("taskDescription", desc);
        taskMap.put("taskDeadlineDate", dateStr);
        taskMap.put("taskDeadlineTime", timeStr);
        taskMap.put("completed", false); // Initialize task as not completed

        // Save task data to Firebase Realtime Database
        FirebaseDatabase.getInstance()
                .getReference("users") // Root node for all users
                .child(uid)           // Specific user's node
                .child("tasks")       // Tasks node under the user
                .child(dateStr)       // Tasks are organized by deadline date
                .child(taskId)        // Unique node for each task
                .setValue(taskMap)    // Set the task data
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(AddPageActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity upon successful task addition
                    } else {
                        // Handle failure to add task
                        String msg = (task.getException() != null) ? task.getException().getMessage() : "Failed to add task";
                        Toast.makeText(AddPageActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }


    /**
     * Schedules a one-time notification reminder for a task at a specific time.
     * This method utilizes {@link AlarmManager} to set an exact alarm that will
     * trigger {@link ReminderReceiver} at the specified {@code reminderTimeMillis}.
     *
     * <p>Special handling is included for Android 12 (API 31) and above:</p>
     * <ul>
     *     <li>It checks if the app has permission to schedule exact alarms using
     *         {@link AlarmManager#canScheduleExactAlarms()}.</li>
     *     <li>If permission is not granted, it informs the user via a {@link Toast}
     *         and attempts to launch the system settings screen where the user can
     *         grant the {@code SCHEDULE_EXACT_ALARM} permission for the app.
     *         The alarm scheduling is then halted.</li>
     * </ul>
     *
     * @param reminderTimeMillis The absolute time in milliseconds (since epoch)
     *                           when the reminder should be triggered.
     * @param taskName           The title of the task, to be displayed in the notification.
     * @param taskDesc           The description of the task, to be displayed as part of the notification.
     * @see ReminderReceiver
     * @see AlarmManager
     */
    private void scheduleReminder(long reminderTimeMillis, String taskName, String taskDesc) {
        // Create an Intent that will be broadcast when the alarm triggers
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("taskTitle", taskName); // Pass task title to the receiver
        intent.putExtra("taskDesc", taskDesc);   // Pass task description to the receiver

        // Create a unique request code for the PendingIntent to allow multiple distinct alarms.
        // Using modulo with Integer.MAX_VALUE to prevent overflow, though System.currentTimeMillis()
        // could be very large. A better approach for true uniqueness across app restarts
        // might involve a counter or database ID.
        int requestCode = (int) (reminderTimeMillis % Integer.MAX_VALUE);

        // Create a PendingIntent to be triggered by the AlarmManager
        // FLAG_UPDATE_CURRENT: If a PendingIntent already exists for this requestCode, keep it but update its extra data.
        // FLAG_IMMUTABLE: Required for Android S (API 31) and above, makes the PendingIntent immutable.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // --- CRUCIAL ANDROID 12+ (API 31+) EXACT ALARM PERMISSION LOGIC ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Check if the app has permission to schedule exact alarms.
                // This permission is required for setExactAndAllowWhileIdle on Android 12+.
                if (!alarmManager.canScheduleExactAlarms()) {
                    // The permission is NOT granted.
                    // Inform the user and guide them to the settings screen to grant permission.
                    Toast.makeText(this, "Permission needed to set exact reminders. Please grant 'Alarms & reminders' permission.", Toast.LENGTH_LONG).show();

                    // Create an intent to open the "Alarms & Reminders" settings page for your app.
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(settingsIntent);

                    // Stop further execution because we can't schedule the exact alarm yet.
                    return;
                }
            }
            // --- END OF ANDROID S+ EXACT ALARM PERMISSION LOGIC ---

            try {
                // If we've reached this point, we have the necessary permission (or it's not needed for this Android version).
                // Schedule the exact and allow-while-idle alarm.
                // RTC_WAKEUP: Wakes up the device and fires the alarm at the specified time.
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeMillis,
                        pendingIntent
                );
                Toast.makeText(this, "Reminder set!", Toast.LENGTH_SHORT).show();

            } catch (SecurityException se) {
                // Catch potential SecurityException if alarm scheduling fails due to permission issues
                // (e.g., if a permission was revoked between the check and the call, though less likely with canScheduleExactAlarms).
                Log.e("Alarm", "SecurityException, could not schedule alarm.", se);
                Toast.makeText(this, "Could not schedule reminder due to a security issue.", Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * Creates a notification channel for task reminders.
     * This is required for delivering notifications on Android 8.0 (Oreo, API 26) and higher.
     * If the device runs on an older Android version, this method does nothing.
     * The channel is configured with high importance, meaning notifications will make a sound
     * and appear on screen.
     */
    private void createNotificationChannel() {
        // Check if the Android version is O or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    REMINDER_CHANNEL_ID,      // Unique ID for the channel
                    REMINDER_CHANNEL_NAME,    // User-visible name of the channel
                    NotificationManager.IMPORTANCE_HIGH // Importance level (e.g., sound, on-screen alert)
            );
            channel.setDescription("Channel for task deadline reminders"); // User-visible description
            // Register the channel with the system
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    /**
     * Requests the {@code android.permission.POST_NOTIFICATIONS} permission from the user.
     * This permission is required for an app to post notifications on Android 13 (Tiramisu, API 33) and higher.
     * If the device runs on an older Android version or the permission is already granted, this method does nothing.
     * The permission request is handled by {@link #requestPermissionLauncher}.
     */
    private void askNotificationPermission() {
        // Check if the Android version is Tiramisu (API 33) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if the permission is not already granted
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Launch the permission request flow
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
```

package com.example.activitystreak;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddPageActivity extends AppCompatActivity {

    public static final String REMINDER_CHANNEL_ID = "TASK_REMINDER_CHANNEL";
    public static final String REMINDER_CHANNEL_NAME = "Task Reminders";

    EditText taskName, taskDescription, deadlineDate, deadlineTime;
    Button addTaskBtn;
    Spinner remainderSpinner;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications will not be shown as permission is denied.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_page);

        taskName = findViewById(R.id.taskName);
        taskDescription = findViewById(R.id.taskDescription);
        deadlineDate = findViewById(R.id.deadlineDate);
        deadlineTime = findViewById(R.id.deadlineTime);
        addTaskBtn = findViewById(R.id.addTaskBtn);
        remainderSpinner = findViewById(R.id.remainderSpinner);

        createNotificationChannel();
        askNotificationPermission();

        String selectedDate = SelectedDateManager.getSelectedDate();
        if (selectedDate == null) {
            Toast.makeText(this, "No date selected, defaulting to today.", Toast.LENGTH_SHORT).show();
            Calendar cal = Calendar.getInstance();
            selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        }
        deadlineDate.setText(selectedDate);

        setupDatePicker();
        setupTimePicker();
        addTaskBtn.setOnClickListener(this::addTaskButtonListener);
    }

    private void setupDatePicker() {
        deadlineDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddPageActivity.this,
                    (view, y, m, d) -> {
                        String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
                        deadlineDate.setText(date);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });
    }

    private void setupTimePicker() {
        deadlineTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    AddPageActivity.this,
                    (view, h, m) -> {
                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", h, m);
                        deadlineTime.setText(formattedTime);
                    },
                    hour, minute, true
            );
            timePickerDialog.show();
        });
    }

    public long getReminderOffsetMillis() {
        // Returns the selected reminder offset time in milliseconds
        // Using 'L' ensures the numbers are treated as 'long' to prevent overflow
        switch (remainderSpinner.getSelectedItemPosition()) {
            case 0: return 15 * 1000L;               // 15 SECONDS FOR TESTING
            case 1: return 60 * 60 * 1000L;          // 1 hour
            case 2: return 2 * 60 * 60 * 1000L;      // 2 hours
            case 3: return 24 * 60 * 60 * 1000L;     // 1 day
            case 4: return 2 * 24 * 60 * 60 * 1000L; // 2 days
            case 5: return 7 * 24 * 60 * 60 * 1000L; // 1 week
            default: return 0; // No reminder
        }
    }

    public void addTaskButtonListener(View view) {
        String name = taskName.getText().toString().trim();
        String desc = taskDescription.getText().toString().trim();
        String dateStr = deadlineDate.getText().toString().trim();
        String timeStr = deadlineTime.getText().toString().trim();
        long reminderOffset = getReminderOffsetMillis();

        if (name.isEmpty() || dateStr.isEmpty() || timeStr.isEmpty()) {
            Toast.makeText(this, "Task name, date, and time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to add a task.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Simplified Reminder Logic ---
        // Schedules a notification for a duration FROM NOW.
        if (reminderOffset > 0) {
            long reminderTimeMillis = System.currentTimeMillis() + reminderOffset;
            scheduleReminder(reminderTimeMillis, name, desc);
        }

        // --- Save to Firebase (This part remains the same) ---
        String uid = currentUser.getUid();
        String taskId = "task_" + System.currentTimeMillis();

        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("taskId", taskId);
        taskMap.put("taskName", name);
        taskMap.put("taskDescription", desc);
        taskMap.put("taskDeadlineDate", dateStr);
        taskMap.put("taskDeadlineTime", timeStr);
        taskMap.put("completed", false);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(dateStr)
                .child(taskId)
                .setValue(taskMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(AddPageActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String msg = (task.getException() != null) ? task.getException().getMessage() : "Failed to add task";
                        Toast.makeText(AddPageActivity.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void scheduleReminder(long reminderTimeMillis, String taskName, String taskDesc) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("taskTitle", taskName);
        intent.putExtra("taskDesc", taskDesc);

        int requestCode = (int) (reminderTimeMillis % Integer.MAX_VALUE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // --- THIS IS THE CRUCIAL, CORRECTED LOGIC ---
            // On Android 12+, we need special permission to schedule exact alarms.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    // The permission is NOT granted.
                    // Inform the user and guide them to the settings screen.
                    Toast.makeText(this, "Permission needed to set reminders.", Toast.LENGTH_LONG).show();

                    // Create an intent to open the "Alarms & Reminders" settings page for your app.
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(settingsIntent);

                    // Stop further execution because we can't schedule the alarm yet.
                    return;
                }
            }
            // --- END OF CORRECTED LOGIC ---

            try {
                // If we've reached this point, we have permission.
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTimeMillis,
                        pendingIntent
                );
                Toast.makeText(this, "Reminder set!", Toast.LENGTH_SHORT).show();

            } catch (SecurityException se) {
                Log.e("Alarm", "SecurityException, could not schedule alarm.", se);
                Toast.makeText(this, "Could not schedule reminder due to a security issue.", Toast.LENGTH_LONG).show();
            }
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    REMINDER_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for task deadline reminders");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
