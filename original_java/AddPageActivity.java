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
