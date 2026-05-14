```java
package com.example.activitystreak;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog; // Explicitly import for AlertDialog in showDeleteConfirmation

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * {@code DayPageActivity} is an Android {@link AppCompatActivity} responsible for displaying
 * and managing tasks for a specific selected date. Users can view, add, edit, complete,
 * and delete tasks associated with the date.
 *
 * <p>This activity interacts with Firebase Realtime Database to perform CRUD operations on tasks.
 * It dynamically generates UI elements ({@link CheckBox} and {@link TextView}) for each task,
 * allowing users to select multiple tasks for batch operations (complete, delete) or a single task
 * for editing. Tasks are sorted by their deadline time and their visual presentation
 * (background color) changes based on their status (pending, completed, failed, selected).</p>
 *
 * <p>It relies on {@link SelectedDateManager} to retrieve the currently selected date
 * and {@link TaskManager} for specific task operations like marking as complete or failed.</p>
 */
public class DayPageActivity extends AppCompatActivity {

    /**
     * The {@link LinearLayout} container where individual task rows ({@code task_item} layout)
     * are dynamically added. This acts as the parent for all task {@link View}s.
     */
    private LinearLayout tasksContainer;

    /**
     * The "Add Task" button. When clicked, it navigates to {@link AddPageActivity}
     * to allow the user to create a new task for the {@link #selectedDate}.
     * A limit of 10 tasks per day is enforced.
     */
    private Button btnAdd;

    /**
     * The "Delete Task(s)" button. When clicked, it initiates a confirmation dialog
     * and, upon confirmation, marks all selected (checked) tasks as deleted in Firebase.
     * Enabled only if at least one task {@link CheckBox} is selected.
     */
    private Button btnDelete;

    /**
     * The "Edit Task" button. When clicked, it navigates to {@link EditPageActivity}
     * to allow the user to modify the details of the *single* selected task.
     * Enabled only if exactly one task {@link CheckBox} is selected.
     */
    private Button btnEdit;

    /**
     * The "Complete Task(s)" button. When clicked, it marks all selected (checked)
     * tasks as completed in Firebase.
     * Enabled only if at least one task {@link CheckBox} is selected.
     */
    private Button btnComplete;

    /**
     * A {@link List} of all {@link CheckBox} instances currently displayed in {@link #tasksContainer}.
     * This list is used to track user selections for batch operations and update button states.
     */
    private final List<CheckBox> checkBoxList = new ArrayList<>();

    /**
     * A {@link String} representing the date for which tasks are being displayed and managed,
     * typically in "yyyy-MM-dd" format. This value is retrieved from {@link SelectedDateManager}.
     */
    private String selectedDate;

    /**
     * Called when the activity is first created.
     * This method initializes the activity, inflates the layout,
     * retrieves the selected date, sets up UI elements, and attaches event listeners.
     * It also initiates the loading of tasks from Firebase.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}. Otherwise, it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Enables edge-to-edge display for modern Android UI
        setContentView(R.layout.day_page); // Sets the activity's UI from the day_page layout

        // Initialize UI components by finding their IDs in the layout
        tasksContainer = findViewById(R.id.tasksContainer);
        btnAdd = findViewById(R.id.btnAdd);
        btnDelete = findViewById(R.id.btnDelete);
        btnEdit = findViewById(R.id.btnEdit);
        btnComplete = findViewById(R.id.btnComplete);

        // Retrieve the selected date from the manager
        selectedDate = SelectedDateManager.getSelectedDate();
        if (selectedDate == null) {
            // If no date is selected, show an error and close the activity
            Toast.makeText(this, "No date selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Display the selected date in a user-friendly format
        TextView dateDisplay = findViewById(R.id.dateText);
        dateDisplay.setText(formatDatePretty(selectedDate));

        // Set click listener for the "Add Task" button
        btnAdd.setOnClickListener(v -> {
            // Enforce a maximum of 10 tasks per day
            if (checkBoxList.size() >= 10) {
                Toast.makeText(this, "Maximum 10 tasks per day", Toast.LENGTH_SHORT).show();
                return;
            }
            // Navigate to AddPageActivity to create a new task
            Intent intent = new Intent(DayPageActivity.this, AddPageActivity.class);
            startActivity(intent);
        });

        // Set click listeners for other action buttons
        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        btnComplete.setOnClickListener(v -> completeSelectedTasks());
        btnEdit.setOnClickListener(v -> editSelectedTask());

        // Load tasks from Firebase for the selected date and update button states
        loadTasksFromFirebase();
        updateButtons();
    }

    /**
     * Called when the activity is about to become visible to the user.
     * This method ensures that the task list is refreshed whenever the activity resumes
     * (e.g., after returning from {@link AddPageActivity} or {@link EditPageActivity}).
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadTasksFromFirebase(); // Reload tasks to reflect any changes made in other activities
    }

    /**
     * Loads tasks for the {@link #selectedDate} from Firebase Realtime Database.
     * This method clears existing task views, retrieves task data, filters out deleted tasks,
     * sorts them by deadline time, determines their status (completed, failed),
     * and dynamically adds new task rows to the {@link #tasksContainer}.
     *
     * <p>It handles marking tasks as "failed" if their deadline has passed and they are
     * not completed, updating this status in Firebase.</p>
     */
    private void loadTasksFromFirebase() {
        // Ensure a user is authenticated before attempting to load tasks
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Construct the Firebase database reference for the user's tasks on the selected date
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .get()
                .addOnSuccessListener(snapshot -> {
                    // Clear all existing task views and the checkbox list before repopulating
                    tasksContainer.removeAllViews();
                    checkBoxList.clear();

                    if (!snapshot.exists()) {
                        // If no tasks exist for this date, update buttons and return
                        updateButtons();
                        return;
                    }

                    // Filter out tasks that are marked as deleted
                    List<DataSnapshot> taskSnaps = new ArrayList<>();
                    for (DataSnapshot t : snapshot.getChildren()) {
                        Boolean deleted = t.child("deleted").getValue(Boolean.class);
                        if (deleted != null && deleted) continue; // Skip deleted tasks
                        taskSnaps.add(t);
                    }

                    // Sort the remaining tasks by their deadline time
                    Collections.sort(taskSnaps, new Comparator<DataSnapshot>() {
                        @Override
                        public int compare(DataSnapshot a, DataSnapshot b) {
                            String t1 = safeTime(a);
                            String t2 = safeTime(b);
                            return t1.compareTo(t2); // Lexicographical comparison for HH:mm
                        }
                    });

                    // Iterate through sorted tasks and add them to the UI
                    for (DataSnapshot t : taskSnaps) {
                        // Extract task details
                        String name = t.child("taskName").getValue(String.class);
                        String time = safeTime(t); // Use safeTime helper to get deadline time
                        Boolean completed = t.child("completed").getValue(Boolean.class);
                        Boolean failed = t.child("failed").getValue(Boolean.class);
                        String taskId = t.child("taskId").getValue(String.class);

                        if (name == null) name = ""; // Default empty string if name is null

                        // Check if the task should be marked as failed based on current time
                        boolean shouldFail = isTaskFailed(selectedDate, time, completed);
                        if (shouldFail && (failed == null || !failed)) {
                            // If it should fail but isn't marked, update its status in Firebase
                            if (taskId != null) {
                                TaskManager.markTaskFailed(selectedDate, taskId, null);
                            } else {
                                // Fallback if taskId is unexpectedly null (should not happen with proper data)
                                t.getRef().child("failed").setValue(true);
                            }
                            failed = true; // Update local variable for immediate UI rendering
                        }

                        if (failed == null) failed = false; // Default to false if failed status is null

                        // Add the task row to the UI
                        addTaskRow(name, time, taskId, completed, failed);
                    }

                    // After all tasks are loaded, update the state of action buttons
                    updateButtons();
                });
    }

    /**
     * Safely retrieves the "taskDeadlineTime" from a {@link DataSnapshot}.
     * If the "taskDeadlineTime" field is missing or null, it defaults to "23:59".
     *
     * @param snap The {@link DataSnapshot} representing a single task.
     * @return A {@link String} representing the task's deadline time (HH:mm format),
     *         or "23:59" if not found.
     */
    private String safeTime(DataSnapshot snap) {
        String t = snap.child("taskDeadlineTime").getValue(String.class);
        if (t == null) t = "23:59"; // Default to end of day if time is not specified
        return t;
    }

    /**
     * Determines if a given task should be considered 'failed'.
     * A task is failed if its deadline has passed and it has not been marked as completed.
     *
     * @param date      The date of the task in "yyyy-MM-dd" format.
     * @param time      The deadline time of the task in "HH:mm" format.
     * @param completed A {@link Boolean} indicating if the task is already completed.
     * @return {@code true} if the task's deadline has passed and it's not completed,
     *         {@code false} otherwise (e.g., still pending, already completed, or parsing error).
     */
    private boolean isTaskFailed(String date, String time, Boolean completed) {
        // A completed task cannot be failed
        if (completed != null && completed) return false;

        try {
            // Combine date and time to create a full datetime string
            String dateTimeStr = date + " " + time;
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            // Parse the deadline string into a long timestamp
            long deadlineMillis = sdf.parse(dateTimeStr).getTime();
            // Compare the deadline with the current system time
            return deadlineMillis < System.currentTimeMillis();
        } catch (ParseException e) {
            // Log the error (though not explicitly shown here) and return false if parsing fails
            // e.g., Log.e("DayPageActivity", "Error parsing date/time for task failure check", e);
            return false;
        } catch (Exception e) {
            // Catch any other potential exceptions during date/time handling
            return false;
        }
    }

    /**
     * Dynamically inflates a task item layout ({@code R.layout.task_item}), populates it
     * with the provided task data, and adds it to the {@link #tasksContainer}.
     * It also configures the {@link CheckBox} listener and applies the initial background.
     *
     * @param taskName  The name of the task to display.
     * @param time      The deadline time of the task (e.g., "14:30").
     * @param taskId    The unique identifier for the task, used as the checkbox's tag for later retrieval.
     * @param completed A {@link Boolean} indicating if the task is completed.
     * @param failed    A {@code boolean} indicating if the task has failed (deadline passed, not completed).
     */
    private void addTaskRow(String taskName, String time, String taskId,
                            Boolean completed, boolean failed) {
        // Inflate the task_item layout
        View row = LayoutInflater.from(this)
                .inflate(R.layout.task_item, tasksContainer, false);

        // Find the CheckBox and TextView within the inflated row
        CheckBox cb = row.findViewById(R.id.checkTask);
        TextView timer = row.findViewById(R.id.taskTimer);

        // Set the text and tag for the CheckBox and the time for the TextView
        cb.setText(taskName);
        timer.setText(time);
        cb.setTag(taskId); // Store taskId in the CheckBox's tag for easy retrieval

        // Apply initial background based on task status
        applyRowBackground(row, completed, failed, false); // Not initially selected

        // Set a listener for when the CheckBox's checked state changes
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Update the row background based on the new checked state and task status
            applyRowBackground(row, completed, failed, isChecked);
            updateButtons(); // Update the state of action buttons
        });

        // Add the CheckBox to the tracking list and the row to the tasks container
        checkBoxList.add(cb);
        tasksContainer.addView(row);
    }

    /**
     * Applies a specific background drawable to a task row based on its current status
     * (selected, completed, failed, or pending). The order of checks determines priority.
     *
     * @param row       The {@link View} representing the task item row.
     * @param completed A {@link Boolean} indicating if the task is completed.
     * @param failed    A {@code boolean} indicating if the task has failed.
     * @param selected  A {@code boolean} indicating if the task's {@link CheckBox} is currently checked (selected by user).
     */
    private void applyRowBackground(View row, Boolean completed, boolean failed, boolean selected) {
        if (selected) {
            row.setBackgroundResource(R.drawable.task_selected_bg); // Highest priority: selected
        } else if (completed != null && completed) {
            row.setBackgroundResource(R.drawable.task_completed_bg); // Second priority: completed
        } else if (failed) {
            row.setBackgroundResource(R.drawable.task_failed_bg); // Third priority: failed
        } else {
            row.setBackgroundResource(R.drawable.task_pending_bg); // Default: pending
        }
    }

    /**
     * Marks all currently selected (checked) tasks as completed in Firebase.
     * It also sets their 'failed' status to {@code false} as a completed task cannot be failed.
     * After updating Firebase, it reloads the tasks to reflect the changes in the UI
     * and shows a {@link Toast} message.
     * If no tasks are selected, a toast informs the user.
     */
    private void completeSelectedTasks() {
        // Ensure user is authenticated
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        int doneCount = 0; // Counter for tasks marked as completed

        // Iterate through all displayed checkboxes
        for (CheckBox cb : checkBoxList) {
            if (!cb.isChecked()) continue; // Skip if not checked

            String taskId = (String) cb.getTag(); // Retrieve taskId from the checkbox's tag
            if (taskId == null) continue; // Skip if taskId is missing

            doneCount++; // Increment count for checked tasks

            // Update Firebase: explicitly set 'failed' to false (a completed task is not failed)
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("tasks")
                    .child(selectedDate)
                    .child(taskId)
                    .child("failed")
                    .setValue(false);

            // Use TaskManager to mark the task as complete (this will also update 'completed' to true)
            TaskManager.completeTask(selectedDate, taskId, null);
        }

        if (doneCount == 0) {
            // Inform user if no tasks were selected for completion
            Toast.makeText(this, "Select at least one task", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reload tasks from Firebase to refresh the UI and update button states
        loadTasksFromFirebase();
        Toast.makeText(this, "Task(s) Completed!", Toast.LENGTH_SHORT).show();
        updateButtons();
    }

    /**
     * Navigates to {@link EditPageActivity} to allow the user to modify the details
     * of the *single* task that is currently selected (checked).
     * If zero or more than one task is selected, this method does nothing.
     */
    private void editSelectedTask() {
        CheckBox selected = null; // To hold the single selected checkbox

        // Find the single checked checkbox
        for (CheckBox cb : checkBoxList) {
            if (cb.isChecked()) {
                // If more than one task is checked, clear selection and return
                if (selected != null) {
                    Toast.makeText(this, "Select only one task to edit", Toast.LENGTH_SHORT).show();
                    return;
                }
                selected = cb;
            }
        }

        // If no task was selected, do nothing
        if (selected == null) {
            Toast.makeText(this, "Select a task to edit", Toast.LENGTH_SHORT).show();
            return;
        }

        String taskId = selected.getTag().toString(); // Get the taskId from the selected checkbox

        // Create an intent to start EditPageActivity, passing the selected date and task ID
        Intent intent = new Intent(DayPageActivity.this, EditPageActivity.class);
        intent.putExtra("selectedDate", selectedDate);
        intent.putExtra("taskId", taskId);
        startActivity(intent);
    }

    /**
     * Marks all currently selected (checked) tasks as 'deleted' in Firebase.
     * It then removes the corresponding task rows from the UI.
     * Tasks are not permanently removed from Firebase but rather marked with a {@code deleted: true} flag.
     */
    private void deleteCheckedTasks() {
        // Ensure user is authenticated
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        List<CheckBox> remaining = new ArrayList<>(); // To hold checkboxes that were not deleted
        int deletedCount = 0;

        // Iterate through all displayed checkboxes
        for (CheckBox cb : checkBoxList) {
            if (cb.isChecked()) {
                String taskId = (String) cb.getTag();
                if (taskId == null) continue;

                // Update 'deleted' status in Firebase to true
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("tasks")
                        .child(selectedDate)
                        .child(taskId)
                        .child("deleted")
                        .setValue(true);

                // Remove the UI row from the tasksContainer
                View parent = (View) cb.getParent();
                tasksContainer.removeView(parent);
                deletedCount++;
            } else {
                remaining.add(cb); // Add non-deleted checkboxes to the 'remaining' list
            }
        }

        // Update the main checkboxList with only the remaining (non-deleted) checkboxes
        checkBoxList.clear();
        checkBoxList.addAll(remaining);

        if (deletedCount > 0) {
            Toast.makeText(this, "Task(s) Deleted!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No tasks selected for deletion.", Toast.LENGTH_SHORT).show();
        }

        updateButtons(); // Update the state of action buttons
    }

    /**
     * Displays an {@link AlertDialog} to confirm if the user truly wishes to delete
     * the selected task(s). If confirmed, {@link #deleteCheckedTasks()} is called.
     */
    private void showDeleteConfirmation() {
        // Count selected tasks to show meaningful message
        int checkedCount = 0;
        for (CheckBox cb : checkBoxList) {
            if (cb.isChecked()) checkedCount++;
        }

        if (checkedCount == 0) {
            Toast.makeText(this, "Select at least one task to delete.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(DayPageActivity.this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete the selected task(s)?")
                .setPositiveButton("Confirm", (dialog, which) -> deleteCheckedTasks()) // Call delete method on confirm
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()) // Dismiss dialog on cancel
                .show();
    }

    /**
     * Updates the enabled state of the action buttons (Delete, Complete, Edit)
     * based on the number of currently selected (checked) tasks.
     * <ul>
     *     <li>{@link #btnDelete} and {@link #btnComplete} are enabled if one or more tasks are checked.</li>
     *     <li>{@link #btnEdit} is enabled only if exactly one task is checked.</li>
     * </ul>
     */
    private void updateButtons() {
        int checked = 0;
        // Count how many checkboxes are currently checked
        for (CheckBox cb : checkBoxList) {
            if (cb.isChecked()) checked++;
        }

        boolean any = checked > 0; // True if at least one task is checked
        btnDelete.setEnabled(any);
        btnComplete.setEnabled(any);
        btnEdit.setEnabled(checked == 1); // Only enable edit if exactly one task is checked
    }

    /**
     * Formats a date string from "yyyy-MM-dd" format to a more readable "MMMM dd, yyyy" format.
     * For example, "2023-10-27" would become "October 27, 2023".
     * If parsing fails, the original date string is returned.
     *
     * @param dateYmd The date string in "yyyy-MM-dd" format.
     * @return A formatted date string (e.g., "October 27, 2023"), or the original string if parsing fails.
     */
    private String formatDatePretty(String dateYmd) {
        try {
            // Define input and output date formats
            SimpleDateFormat inFmt =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outFmt =
                    new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            // Parse the input date and then format it to the desired output
            return outFmt.format(inFmt.parse(dateYmd));
        } catch (ParseException e) {
            // If parsing fails, return the original date string
            // e.g., Log.e("DayPageActivity", "Error parsing date for pretty format", e);
            return dateYmd;
        }
    }
}
```

package com.example.activitystreak;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DayPageActivity extends AppCompatActivity {

    private LinearLayout tasksContainer;
    private Button btnAdd, btnDelete, btnEdit, btnComplete;

    private final List<CheckBox> checkBoxList = new ArrayList<>();

    private String selectedDate;   

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.day_page);

        tasksContainer = findViewById(R.id.tasksContainer);
        btnAdd = findViewById(R.id.btnAdd);
        btnDelete = findViewById(R.id.btnDelete);
        btnEdit = findViewById(R.id.btnEdit);
        btnComplete = findViewById(R.id.btnComplete);

        selectedDate = SelectedDateManager.getSelectedDate();
        if (selectedDate == null) {
            Toast.makeText(this, "No date selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView dateDisplay = findViewById(R.id.dateText);
        dateDisplay.setText(formatDatePretty(selectedDate));

        btnAdd.setOnClickListener(v -> {
            if (checkBoxList.size() >= 10) {
                Toast.makeText(this, "Maximum 10 tasks per day", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(DayPageActivity.this, AddPageActivity.class);
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        btnComplete.setOnClickListener(v -> completeSelectedTasks());
        btnEdit.setOnClickListener(v -> editSelectedTask());

        loadTasksFromFirebase();
        updateButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasksFromFirebase();
    }

    private void loadTasksFromFirebase() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .child(selectedDate)
                .get()
                .addOnSuccessListener(snapshot -> {

                    tasksContainer.removeAllViews();
                    checkBoxList.clear();

                    if (!snapshot.exists()) {
                        updateButtons();
                        return;
                    }

                    List<DataSnapshot> taskSnaps = new ArrayList<>();
                    for (DataSnapshot t : snapshot.getChildren()) {
                        Boolean deleted = t.child("deleted").getValue(Boolean.class);
                        if (deleted != null && deleted) continue;
                        taskSnaps.add(t);
                    }

                    Collections.sort(taskSnaps, new Comparator<DataSnapshot>() {
                        @Override
                        public int compare(DataSnapshot a, DataSnapshot b) {
                            String t1 = safeTime(a);
                            String t2 = safeTime(b);
                            return t1.compareTo(t2);
                        }
                    });

                    for (DataSnapshot t : taskSnaps) {

                        String name = t.child("taskName").getValue(String.class);
                        String time = safeTime(t);
                        Boolean completed = t.child("completed").getValue(Boolean.class);
                        Boolean failed = t.child("failed").getValue(Boolean.class);
                        String taskId = t.child("taskId").getValue(String.class);

                        if (name == null) name = "";

                        boolean shouldFail = isTaskFailed(selectedDate, time, completed);
                        if (shouldFail && (failed == null || !failed)) {

                            if (taskId != null) {
                                TaskManager.markTaskFailed(selectedDate, taskId, null);
                            } else {
                                t.getRef().child("failed").setValue(true);
                            }
                            failed = true;
                        }

                        if (failed == null) failed = false;

                        addTaskRow(name, time, taskId, completed, failed);
                    }

                    updateButtons();
                });
    }

    private String safeTime(DataSnapshot snap) {
        String t = snap.child("taskDeadlineTime").getValue(String.class);
        if (t == null) t = "23:59";
        return t;
    }

    private boolean isTaskFailed(String date, String time, Boolean completed) {

        if (completed != null && completed) return false;

        try {
            String dateTimeStr = date + " " + time;
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            long deadlineMillis = sdf.parse(dateTimeStr).getTime();
            return deadlineMillis < System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }

    private void addTaskRow(String taskName, String time, String taskId,
                            Boolean completed, boolean failed) {

        View row = LayoutInflater.from(this)
                .inflate(R.layout.task_item, tasksContainer, false);

        CheckBox cb = row.findViewById(R.id.checkTask);
        TextView timer = row.findViewById(R.id.taskTimer);

        cb.setText(taskName);
        timer.setText(time);
        cb.setTag(taskId);

        applyRowBackground(row, completed, failed, false);

        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            applyRowBackground(row, completed, failed, isChecked);
            updateButtons();
        });

        checkBoxList.add(cb);
        tasksContainer.addView(row);
    }

    private void applyRowBackground(View row, Boolean completed, boolean failed, boolean selected) {
        if (selected) {
            row.setBackgroundResource(R.drawable.task_selected_bg);
        } else if (completed != null && completed) {
            row.setBackgroundResource(R.drawable.task_completed_bg);
        } else if (failed) {
            row.setBackgroundResource(R.drawable.task_failed_bg);
        } else {
            row.setBackgroundResource(R.drawable.task_pending_bg);
        }
    }


    private void completeSelectedTasks() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        int doneCount = 0;

        for (CheckBox cb : checkBoxList) {
            if (!cb.isChecked()) continue;

            String taskId = (String) cb.getTag();
            if (taskId == null) continue;

            doneCount++;


            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("tasks")
                    .child(selectedDate)
                    .child(taskId)
                    .child("failed")
                    .setValue(false);


            TaskManager.completeTask(selectedDate, taskId, null);
        }

        if (doneCount == 0) {
            Toast.makeText(this, "Select at least one task", Toast.LENGTH_SHORT).show();
            return;
        }

        loadTasksFromFirebase();
        Toast.makeText(this, "Task(s) Completed!", Toast.LENGTH_SHORT).show();
        updateButtons();
    }

    private void editSelectedTask() {
        CheckBox selected = null;

        for (CheckBox cb : checkBoxList) {
            if (cb.isChecked()) {
                selected = cb;
                break;
            }
        }
        if (selected == null) return;

        String taskId = selected.getTag().toString();

        Intent intent = new Intent(DayPageActivity.this, EditPageActivity.class);
        intent.putExtra("selectedDate", selectedDate);
        intent.putExtra("taskId", taskId);
        startActivity(intent);
    }

    private void deleteCheckedTasks() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        List<CheckBox> remaining = new ArrayList<>();

        for (CheckBox cb : checkBoxList) {
            if (cb.isChecked()) {
                String taskId = (String) cb.getTag();
                if (taskId == null) continue;

                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("tasks")
                        .child(selectedDate)
                        .child(taskId)
                        .child("deleted")
                        .setValue(true);

                View parent = (View) cb.getParent();
                tasksContainer.removeView(parent);
            } else {
                remaining.add(cb);
            }
        }

        checkBoxList.clear();
        checkBoxList.addAll(remaining);
        updateButtons();
    }

    private void showDeleteConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(DayPageActivity.this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete the selected task(s)?")
                .setPositiveButton("Confirm", (dialog, which) -> deleteCheckedTasks())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateButtons() {
        int checked = 0;
        for (CheckBox cb : checkBoxList) {
            if (cb.isChecked()) checked++;
        }

        boolean any = checked > 0;
        btnDelete.setEnabled(any);
        btnComplete.setEnabled(any);
        btnEdit.setEnabled(checked == 1);
    }

    private String formatDatePretty(String dateYmd) {
        try {
            SimpleDateFormat inFmt =
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outFmt =
                    new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            return outFmt.format(inFmt.parse(dateYmd));
        } catch (ParseException e) {
            return dateYmd;
        }
    }
}
