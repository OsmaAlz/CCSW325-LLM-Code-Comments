package com.example.activitystreak;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Locale;

public class EditPageActivity extends AppCompatActivity {

    private EditText taskName, taskDescription, deadlineDate, deadlineTime;
    private Spinner reminderSpinner;
    private Button editTaskBtn, cancelTaskBtn;

    private String selectedDate;
    private String taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_page);


        taskName = findViewById(R.id.taskName);
        taskDescription = findViewById(R.id.taskDescription);
        deadlineDate = findViewById(R.id.deadlineDate);
        deadlineTime = findViewById(R.id.deadlineTime);
        reminderSpinner = findViewById(R.id.remainderSpinner);

        editTaskBtn = findViewById(R.id.editTaskBtn);
        cancelTaskBtn = findViewById(R.id.cancelTaskBtn);


        selectedDate = getIntent().getStringExtra("selectedDate");
        taskId = getIntent().getStringExtra("taskId");

        if (selectedDate == null || taskId == null) {
            Toast.makeText(this, "Cannot edit task. Missing data.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadTaskData();


        deadlineDate.setOnClickListener(v -> showDatePicker());
        deadlineTime.setOnClickListener(v -> showTimePicker());

        cancelTaskBtn.setOnClickListener(v -> finish());
        editTaskBtn.setOnClickListener(v -> saveChanges());
    }

    private void loadTaskData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    taskName.setText(snapshot.child("taskName").getValue(String.class));
                    taskDescription.setText(snapshot.child("taskDescription").getValue(String.class));

                    String d = snapshot.child("taskDeadlineDate").getValue(String.class);
                    String t = snapshot.child("taskDeadlineTime").getValue(String.class);

                    if (d != null) deadlineDate.setText(d);
                    if (t != null) deadlineTime.setText(t);


                    String reminder = snapshot.child("reminder").getValue(String.class);
                    if (reminder != null) {
                        setSpinnerToValue(reminderSpinner, reminder);
                    }
                });
    }

    private void setSpinnerToValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }


    private void showDatePicker() {
        Calendar c = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, y, m, d) ->
                        deadlineDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)),
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }


    private void showTimePicker() {
        Calendar c = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, h, m) ->
                        deadlineTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true
        );

        dialog.show();
    }


    private void saveChanges() {

        String newName = taskName.getText().toString().trim();
        String newDesc = taskDescription.getText().toString().trim();
        String newDate = deadlineDate.getText().toString().trim();
        String newTime = deadlineTime.getText().toString().trim();
        String newReminder = reminderSpinner.getSelectedItem().toString();

        if (newName.isEmpty()) {
            taskName.setError("Task name required");
            return;
        }
        if (newDate.isEmpty()) {
            Toast.makeText(this, "Select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newTime.isEmpty()) {
            Toast.makeText(this, "Select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();


        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("taskName").setValue(newName);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("taskDescription").setValue(newDesc);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("taskDeadlineDate").setValue(newDate);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("taskDeadlineTime").setValue(newTime);

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("reminder").setValue(newReminder);

        // Mark as edited (optional)
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("edited").setValue(true);

        Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();

        setResult(RESULT_OK);
        finish();
    }
}