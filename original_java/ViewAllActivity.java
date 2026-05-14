package com.example.activitystreak;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ViewAllActivity extends AppCompatActivity {

    private LinearLayout tasksContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_all_page);

        tasksContainer = findViewById(R.id.tasksContainer);

        loadPendingTasksOnly();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingTasksOnly();
    }


    private void loadPendingTasksOnly() {

        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .get()
                .addOnSuccessListener(snapshot -> {

                    tasksContainer.removeAllViews();
                    List<TaskItem> pendingTasks = new ArrayList<>();

                    for (DataSnapshot daySnap : snapshot.getChildren()) {

                        String date = daySnap.getKey();
                        if (date == null) continue;

                        for (DataSnapshot t : daySnap.getChildren()) {

                            Boolean deleted = t.child("deleted").getValue(Boolean.class);
                            if (deleted != null && deleted) continue;

                            Boolean completed = t.child("completed").getValue(Boolean.class);
                            Boolean failedFlag = t.child("failed").getValue(Boolean.class);


                            if (completed != null && completed) continue;


                            if (failedFlag != null && failedFlag) continue;

                            String name = t.child("taskName").getValue(String.class);
                            String desc = t.child("taskDescription").getValue(String.class);
                            String time = t.child("taskDeadlineTime").getValue(String.class);
                            String taskId = t.child("taskId").getValue(String.class);

                            if (name == null) continue;
                            if (time == null) time = "23:59";

                            pendingTasks.add(new TaskItem(
                                    name,
                                    (desc == null ? "" : desc),
                                    time,
                                    date,
                                    taskId,
                                    false,
                                    false
                            ));
                        }
                    }


                    Collections.sort(pendingTasks, new Comparator<TaskItem>() {
                        @Override
                        public int compare(TaskItem a, TaskItem b) {
                            int c = a.date.compareTo(b.date);
                            if (c == 0) return a.time.compareTo(b.time);
                            return c;
                        }
                    });

                    for (TaskItem item : pendingTasks) {
                        addTaskRow(item);
                    }
                });
    }


    private void addTaskRow(TaskItem item) {

        View row = LayoutInflater.from(this)
                .inflate(R.layout.task_row, tasksContainer, false);

        TextView titleTv = row.findViewById(R.id.taskTitle);
        TextView descTv = row.findViewById(R.id.taskDescription);
        TextView deadlineTv = row.findViewById(R.id.taskDeadline);

        titleTv.setText(item.name);

        if (item.desc.trim().isEmpty()) {
            descTv.setVisibility(View.GONE);
        } else {
            descTv.setVisibility(View.VISIBLE);
            descTv.setText(item.desc);
        }

        deadlineTv.setText(item.date + "  " + item.time);


        row.setBackgroundResource(R.drawable.task_pending_bg);

        tasksContainer.addView(row);
    }
}
