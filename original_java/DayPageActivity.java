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
