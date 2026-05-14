```java
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

/**
 * {@code EditPageActivity} is an Android Activity responsible for displaying and allowing users
 * to edit the details of an existing task.
 * <p>
 * This activity retrieves task data from Firebase Realtime Database based on a
 * {@code selectedDate} and {@code taskId} passed via Intent extras. Users can modify the
 * task's name, description, deadline date, deadline time, and reminder setting.
 * All changes are validated and then persisted back to the Firebase Realtime Database.
 * </p>
 * <p>
 * It integrates with {@link FirebaseAuth} for user authentication to determine the correct
 * Firebase path and {@link FirebaseDatabase} for reading and writing task information.
 * Date and time selection are handled by {@link DatePickerDialog} and {@link TimePickerDialog}
 * respectively, providing a user-friendly interface for setting deadlines.
 * </p>
 * <p>
 * Upon successful saving of changes, the activity finishes and returns {@code RESULT_OK} to
 * the calling activity. If essential data ({@code selectedDate} or {@code taskId}) is missing
 * from the Intent, the activity displays an error and finishes immediately.
 * </p>
 *
 * @author Your Name/Team Name (if applicable)
 * @version 1.0
 * @since 2023-10-27 (or relevant date)
 */
public class EditPageActivity extends AppCompatActivity {

    /**
     * EditText for the task's name.
     */
    private EditText taskName;
    /**
     * EditText for the task's description.
     */
    private EditText taskDescription;
    /**
     * EditText for displaying and setting the task's deadline date.
     * Tapping this field opens a {@link DatePickerDialog}.
     */
    private EditText deadlineDate;
    /**
     * EditText for displaying and setting the task's deadline time.
     * Tapping this field opens a {@link TimePickerDialog}.
     */
    private EditText deadlineTime;
    /**
     * Spinner for selecting the reminder setting for the task.
     */
    private Spinner reminderSpinner;
    /**
     * Button to save the edited task details to Firebase.
     */
    private Button editTaskBtn;
    /**
     * Button to cancel editing and return to the previous activity without saving.
     */
    private Button cancelTaskBtn;

    /**
     * Stores the date associated with the task being edited,
     * typically received from the calling activity via Intent.
     * This forms part of the Firebase database path.
     * Format: YYYY-MM-DD.
     */
    private String selectedDate;
    /**
     * Stores the unique ID of the task being edited,
     * typically received from the calling activity via Intent.
     * This forms part of the Firebase database path.
     */
    private String taskId;

    /**
     * Called when the activity is first created.
     * <p>
     * This method initializes the activity, sets the content view, and performs the following:
     * <ul>
     *     <li>Inflates the layout {@code R.layout.edit_page}.</li>
     *     <li>Initializes all UI components (EditTexts, Spinner, Buttons) by finding their IDs.</li>
     *     <li>Retrieves {@code selectedDate} and {@code taskId} from the incoming Intent extras.
     *         If either is null, it displays an error Toast and finishes the activity,
     *         as these are crucial for loading and saving task data.</li>
     *     <li>Calls {@link #loadTaskData()} to populate the UI fields with existing task details.</li>
     *     <li>Sets up {@link android.view.View.OnClickListener} for {@code deadlineDate} to show
     *         a {@link DatePickerDialog}.</li>
     *     <li>Sets up {@link android.view.View.OnClickListener} for {@code deadlineTime} to show
     *         a {@link TimePickerDialog}.</li>
     *     <li>Sets up {@link android.view.View.OnClickListener} for {@code cancelTaskBtn} to
     *         simply finish the activity.</li>
     *     <li>Sets up {@link android.view.View.OnClickListener} for {@code editTaskBtn} to
     *         initiate the {@link #saveChanges()} process.</li>
     * </ul>
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState(Bundle)}.
     *                           Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_page);

        // Initialize UI components
        taskName = findViewById(R.id.taskName);
        taskDescription = findViewById(R.id.taskDescription);
        deadlineDate = findViewById(R.id.deadlineDate);
        deadlineTime = findViewById(R.id.deadlineTime);
        reminderSpinner = findViewById(R.id.remainderSpinner);

        editTaskBtn = findViewById(R.id.editTaskBtn);
        cancelTaskBtn = findViewById(R.id.cancelTaskBtn);

        // Retrieve data from Intent
        selectedDate = getIntent().getStringExtra("selectedDate");
        taskId = getIntent().getStringExtra("taskId");

        // Validate essential incoming data
        if (selectedDate == null || taskId == null) {
            Toast.makeText(this, "Cannot edit task. Missing data.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if essential data is missing
            return;
        }

        // Load existing task data from Firebase to populate the fields
        loadTaskData();

        // Set up click listeners for date and time pickers
        deadlineDate.setOnClickListener(v -> showDatePicker());
        deadlineTime.setOnClickListener(v -> showTimePicker());

        // Set up click listeners for action buttons
        cancelTaskBtn.setOnClickListener(v -> finish()); // Cancel action
        editTaskBtn.setOnClickListener(v -> saveChanges()); // Save changes action
    }

    /**
     * Loads the existing task data from Firebase Realtime Database and populates
     * the respective UI elements (EditTexts and Spinner).
     * <p>
     * The method constructs a Firebase database reference using the current user's UID,
     * the {@link #selectedDate}, and the {@link #taskId}. It then performs a
     * {@code get()} operation to retrieve the task snapshot.
     * Upon successful retrieval:
     * <ul>
     *     <li>The {@code taskName} and {@code taskDescription} EditTexts are set.</li>
     *     <li>The {@code deadlineDate} and {@code deadlineTime} EditTexts are set,
     *         with null checks to prevent {@link NullPointerException}.</li>
     *     <li>The {@code reminderSpinner} is set to the previously saved reminder value
     *         using the {@link #setSpinnerToValue(Spinner, String)} helper method.</li>
     * </ul>
     * Error handling for data retrieval (e.g., if task doesn't exist) is implicitly
     * handled by the `addOnSuccessListener` not executing its body if snapshot is empty,
     * or it would require an `addOnFailureListener`.
     * </p>
     *
     * @apiNote Requires the user to be authenticated with Firebase (`FirebaseAuth.getInstance().getCurrentUser()`
     *          must not return null).
     */
    private void loadTaskData() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance()
                .getReference("users") // Root node for all users
                .child(uid)           // Specific user's data
                .child("tasks")       // User's tasks node
                .child(selectedDate)  // Tasks for a specific date
                .child(taskId)        // The specific task to edit
                .get()                // Retrieve the data once
                .addOnSuccessListener(snapshot -> {
                    // Populate EditTexts with retrieved values
                    taskName.setText(snapshot.child("taskName").getValue(String.class));
                    taskDescription.setText(snapshot.child("taskDescription").getValue(String.class));

                    String d = snapshot.child("taskDeadlineDate").getValue(String.class);
                    String t = snapshot.child("taskDeadlineTime").getValue(String.class);

                    // Set deadline date and time, handling potential nulls
                    if (d != null) deadlineDate.setText(d);
                    if (t != null) deadlineTime.setText(t);

                    // Set the spinner to the saved reminder value
                    String reminder = snapshot.child("reminder").getValue(String.class);
                    if (reminder != null) {
                        setSpinnerToValue(reminderSpinner, reminder);
                    }
                });
    }

    /**
     * Helper method to programmatically set the selection of a {@link Spinner}
     * based on a given string value.
     * <p>
     * This method iterates through all items in the spinner's adapter. If a match
     * is found (case-sensitive string equality), the spinner's selection is set
     * to that item's position, and the loop terminates.
     * </p>
     *
     * @param spinner The {@link Spinner} control whose selection needs to be set.
     * @param value   The string value to match against the spinner's items.
     *                This value should correspond to one of the displayable items
     *                in the spinner's adapter.
     */
    private void setSpinnerToValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break; // Exit loop once the value is found and set
            }
        }
    }

    /**
     * Displays a {@link DatePickerDialog} to allow the user to select a deadline date.
     * <p>
     * When the user selects a date and clicks 'OK', the chosen date is formatted
     * as "YYYY-MM-DD" (e.g., "2023-10-27") and displayed in the {@code deadlineDate}
     * EditText. The dialog is initialized with the current date by default.
     * </p>
     */
    private void showDatePicker() {
        // Get current date to initialize the picker
        Calendar c = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                // OnDateSetListener: called when the user sets a date
                (view, y, m, d) ->
                        // Format the date and set it to the EditText
                        deadlineDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)), // Month is 0-indexed
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    /**
     * Displays a {@link TimePickerDialog} to allow the user to select a deadline time.
     * <p>
     * When the user selects a time and clicks 'OK', the chosen time is formatted
     * as "HH:MM" (e.g., "14:30") and displayed in the {@code deadlineTime}
     * EditText. The dialog is initialized with the current time by default and
     * uses a 24-hour format.
     * </p>
     */
    private void showTimePicker() {
        // Get current time to initialize the picker
        Calendar c = Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                // OnTimeSetListener: called when the user sets a time
                (view, h, m) ->
                        // Format the time and set it to the EditText
                        deadlineTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                true // true for 24-hour format
        );

        dialog.show();
    }

    /**
     * Validates the input fields and saves the updated task details to Firebase Realtime Database.
     * <p>
     * This method performs the following steps:
     * <ol>
     *     <li>Retrieves the trimmed text from {@code taskName}, {@code taskDescription},
     *         {@code deadlineDate}, {@code deadlineTime}, and the selected item from
     *         {@code reminderSpinner}.</li>
     *     <li>Performs basic validation:
     *         <ul>
     *             <li>Checks if {@code taskName} is empty. If so, sets an error and returns.</li>
     *             <li>Checks if {@code deadlineDate} is empty. If so, shows a Toast and returns.</li>
     *             <li>Checks if {@code deadlineTime} is empty. If so, shows a Toast and returns.</li>
     *         </ul>
     *     </li>
     *     <li>If validation passes, it constructs the Firebase database path for the specific task.</li>
     *     <li>Updates each individual field (taskName, taskDescription, taskDeadlineDate,
     *         taskDeadlineTime, reminder, and a new "edited" flag) in Firebase. This is done
     *         by calling {@code setValue()} for each child node.</li>
     *     <li>Displays a "Task updated!" Toast message upon successful updates.</li>
     *     <li>Sets the activity result to {@code RESULT_OK} to indicate success to the calling activity.</li>
     *     <li>Finishes the activity, returning to the previous screen.</li>
     * </ol>
     * </p>
     *
     * @apiNote Requires the user to be authenticated with Firebase.
     *          Multiple individual {@code setValue()} calls are used here, which means
     *          Firebase will process them as separate write operations. For atomic updates
     *          of multiple fields, a {@code updateChildren()} call with a Map would be more efficient.
     */
    private void saveChanges() {
        // Retrieve and trim values from UI fields
        String newName = taskName.getText().toString().trim();
        String newDesc = taskDescription.getText().toString().trim();
        String newDate = deadlineDate.getText().toString().trim();
        String newTime = deadlineTime.getText().toString().trim();
        String newReminder = reminderSpinner.getSelectedItem().toString();

        // Input validation
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

        // Get current user's UID for Firebase path
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Construct Firebase base reference for the specific task
        // Note: Using multiple setValue calls for individual fields.
        // A single updateChildren() call with a Map would be more efficient for atomic updates.

        // Update task name
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("taskName").setValue(newName);

        // Update task description
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("taskDescription").setValue(newDesc);

        // Update deadline date
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("taskDeadlineDate").setValue(newDate);

        // Update deadline time
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("taskDeadlineTime").setValue(newTime);

        // Update reminder setting
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("reminder").setValue(newReminder);

        // Mark task as edited (optional flag)
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .child(taskId)
                .child("edited").setValue(true);

        // Notify user and finish activity
        Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show();

        setResult(RESULT_OK); // Indicate success to the calling activity
        finish(); // Close the activity
    }
}
```

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