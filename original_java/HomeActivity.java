package com.example.activitystreak;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView calendarRecycler;
    private CalendarAdapter calendarAdapter;

    private ProgressBar progressBar;
    private TextView streakCount;
    private TextView txtMonthYear;
    private TextView streakSubtitle;

    private Calendar calendar;
    private Map<String, CalendarAdapter.DayStatus> statusMap = new HashMap<>();

    // Upcoming tasks widgets
    private View card1, card2, card3;
    private TextView firstTask, firstTaskTime;
    private TextView secondTask, secondTaskTime;
    private TextView thirdTask, thirdTaskTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);


        calendarRecycler = findViewById(R.id.calendarRecyclerView);
        txtMonthYear = findViewById(R.id.txtMonthYear);
        progressBar = findViewById(R.id.progressBar);
        streakCount = findViewById(R.id.streakCount);
        streakSubtitle = findViewById(R.id.streakSubtitle);


        card1 = findViewById(R.id.taskCard);
        card2 = findViewById(R.id.taskCard2);
        card3 = findViewById(R.id.taskCard3);

        firstTask = findViewById(R.id.first_task);
        firstTaskTime = findViewById(R.id.first_task_rem_time);

        secondTask = findViewById(R.id.second_task);
        secondTaskTime = findViewById(R.id.second_task_rem_time);

        thirdTask = findViewById(R.id.third_task);
        thirdTaskTime = findViewById(R.id.third_task_rem_time);


        calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        calendarAdapter = new CalendarAdapter();
        calendarRecycler.setLayoutManager(new GridLayoutManager(this, 7));
        calendarRecycler.setAdapter(calendarAdapter);

        calendarAdapter.setOnDayClickListener(day -> {
            String date = String.format(Locale.getDefault(),
                    "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    day
            );

            SelectedDateManager.setSelectedDate(date);
            startActivity(new Intent(HomeActivity.this, DayPageActivity.class));
        });


        ImageView btnPrev = findViewById(R.id.btnPrevMonth);
        ImageView btnNext = findViewById(R.id.btnNextMonth);

        btnPrev.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateCalendar();
        });

        btnNext.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateCalendar();
        });


        loadStreakBar();
        updateCalendar();
        loadUpcomingTasks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStreakBar();
        updateCalendar();
        loadUpcomingTasks();
    }

    private void updateCalendar() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        txtMonthYear.setText(
                calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                        + " " + year
        );

        List<String> monthDays = buildCalendarDays(calendar);
        calendarAdapter.setYearMonth(year, month);
        calendarAdapter.setDays(monthDays);

        loadStatusColors(year, month);
    }

    private List<String> buildCalendarDays(Calendar cal) {
        List<String> result = new ArrayList<>();

        Calendar temp = (Calendar) cal.clone();

        int daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
        int firstDayWeek = temp.get(Calendar.DAY_OF_WEEK);

        // padding before first day
        for (int i = 1; i < firstDayWeek; i++) result.add("");

        for (int d = 1; d <= daysInMonth; d++) result.add(String.valueOf(d));

        return result;
    }

    private void loadStatusColors(int year, int month) {
        statusMap.clear();

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(FirebaseAuth.getInstance().getUid())
                .child("tasks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        for (DataSnapshot dateSnap : snapshot.getChildren()) {

                            String dateKey = dateSnap.getKey();
                            if (dateKey == null) continue;

                            int y = Integer.parseInt(dateKey.substring(0, 4));
                            int m = Integer.parseInt(dateKey.substring(5, 7)) - 1;

                            if (y != year || m != month) continue;

                            boolean hasPending = false;
                            boolean hasCompleted = false;
                            boolean hasFailed = false;

                            for (DataSnapshot taskSnap : dateSnap.getChildren()) {

                                Boolean deleted = taskSnap.child("deleted").getValue(Boolean.class);
                                if (deleted != null && deleted) continue;

                                Boolean completed = taskSnap.child("completed").getValue(Boolean.class);
                                Boolean failed = taskSnap.child("failed").getValue(Boolean.class);

                                if (completed != null && completed) hasCompleted = true;
                                else if (failed != null && failed) hasFailed = true;
                                else hasPending = true;
                            }

                            CalendarAdapter.DayStatus status = CalendarAdapter.DayStatus.NONE;

                            if (hasPending) status = CalendarAdapter.DayStatus.PENDING;
                            else if (hasCompleted && !hasFailed) status = CalendarAdapter.DayStatus.COMPLETED;
                            else if (!hasCompleted && hasFailed) status = CalendarAdapter.DayStatus.FAILED;

                            statusMap.put(dateKey, status);
                        }

                        calendarAdapter.setStatusMap(statusMap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) { }
                });
    }

    private void loadStreakBar() {
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(FirebaseAuth.getInstance().getUid())
                .get()
                .addOnSuccessListener(snap -> {

                    Long streak = snap.child("currentStreak").getValue(Long.class);
                    Double percent = snap.child("streakBarPercent").getValue(Double.class);

                    long streakValue = (streak == null ? 0 : streak);

                    streakCount.setText(String.valueOf(streakValue));
                    progressBar.setProgress(percent == null ? 0 : percent.intValue());

                    // 🔥 milestone name purely from the streak value
                    streakSubtitle.setText(getMilestoneName(streakValue));
                });
    }

    private String getMilestoneName(long streak) {


        if (streak == 0)
            return "Complete tasks to achieve milestones";

        if (streak >= 170) return "Top Performer";
        if (streak >= 120) return "Pro";
        if (streak >= 90)  return "Expert";
        if (streak >= 60)  return "High-Achiever";
        if (streak >= 35)  return "Performer";
        if (streak >= 20)  return "Achiever";
        if (streak >= 5)   return "Learner";

        // 1–4
        return "Starter";
    }

    private void loadUpcomingTasks() {

        String uid = FirebaseAuth.getInstance().getUid();

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .get()
                .addOnSuccessListener(snapshot -> {

                    List<TaskPreview> list = new ArrayList<>();

                    for (DataSnapshot daySnap : snapshot.getChildren()) {

                        String date = daySnap.getKey();
                        if (date == null) continue;

                        for (DataSnapshot t : daySnap.getChildren()) {

                            Boolean deleted = t.child("deleted").getValue(Boolean.class);
                            Boolean completed = t.child("completed").getValue(Boolean.class);
                            Boolean failed = t.child("failed").getValue(Boolean.class);

                            if (deleted != null && deleted) continue;
                            if (completed != null && completed) continue;
                            if (failed != null && failed) continue;

                            String name = t.child("taskName").getValue(String.class);
                            String time = t.child("taskDeadlineTime").getValue(String.class);

                            if (name == null || time == null) continue;

                            list.add(new TaskPreview(name, time, date));
                        }
                    }

                    // sort by date + time
                    Collections.sort(list, (a, b) ->
                            (a.date + " " + a.time).compareTo(b.date + " " + b.time)
                    );

                    fillTaskCard(1, list.size() > 0 ? list.get(0) : null);
                    fillTaskCard(2, list.size() > 1 ? list.get(1) : null);
                    fillTaskCard(3, list.size() > 2 ? list.get(2) : null);
                });
    }

    private void fillTaskCard(int index, TaskPreview t) {

        View card;
        TextView taskName, taskTime;

        if (index == 1) {
            card = card1; taskName = firstTask; taskTime = firstTaskTime;
        } else if (index == 2) {
            card = card2; taskName = secondTask; taskTime = secondTaskTime;
        } else {
            card = card3; taskName = thirdTask; taskTime = thirdTaskTime;
        }

        if (t == null) {
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);
        taskName.setText(t.name);
        taskTime.setText(t.time);

        card.setOnClickListener(v -> {
            SelectedDateManager.setSelectedDate(t.date);
            startActivity(new Intent(HomeActivity.this, DayPageActivity.class));
        });
    }

    public static class TaskPreview {
        public String name;
        public String time;
        public String date;

        public TaskPreview(String name, String time, String date) {
            this.name = name;
            this.time = time;
            this.date = date;
        }
    }

    public void goToToday(View view) {
        Calendar now = Calendar.getInstance();
        SelectedDateManager.setSelectedDate(String.format(Locale.getDefault(),
                "%04d-%02d-%02d",
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH)
        ));
        startActivity(new Intent(this, DayPageActivity.class));
    }

    public void viewAllButtonListener(View view) {
        startActivity(new Intent(this, ViewAllActivity.class));
    }

    public void logoutButton(View view) {
        Intent intent = new Intent(this, LogoutActivity.class);
        startActivity(intent);
    }
}
