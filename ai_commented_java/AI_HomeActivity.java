```java
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

/**
 * {@code HomeActivity} serves as the main dashboard for the Activity Streak application.
 * It displays a monthly calendar, the user's current activity streak and progress,
 * and a preview of upcoming tasks.
 * <p>
 * This activity interacts heavily with Firebase Realtime Database to fetch and display
 * user-specific data, including task statuses for the calendar, streak count,
 * and details of upcoming tasks. It leverages a {@link RecyclerView} with a
 * {@link GridLayoutManager} and {@link CalendarAdapter} to render the monthly calendar.
 * </p>
 * <p>
 * Users can navigate through months, click on a day to view its specific tasks
 * (via {@link DayPageActivity}), and interact with buttons to view all tasks
 * (via {@link ViewAllActivity}) or log out (via {@link LogoutActivity}).
 * </p>
 *
 * @author [Your Name/Team Name]
 * @version 1.0
 * @see DayPageActivity
 * @see ViewAllActivity
 * @see LogoutActivity
 * @see CalendarAdapter
 * @see SelectedDateManager
 */
public class HomeActivity extends AppCompatActivity {

    /**
     * RecyclerView responsible for displaying the monthly calendar grid.
     */
    private RecyclerView calendarRecycler;
    /**
     * Adapter for managing and displaying individual day cells within the calendar RecyclerView.
     */
    private CalendarAdapter calendarAdapter;

    /**
     * ProgressBar to visually represent the user's streak progress towards the next milestone.
     */
    private ProgressBar progressBar;
    /**
     * TextView to display the user's current streak count.
     */
    private TextView streakCount;
    /**
     * TextView to display the currently viewed month and year in the calendar.
     */
    private TextView txtMonthYear;
    /**
     * TextView to display a motivational subtitle or milestone name based on the current streak.
     */
    private TextView streakSubtitle;

    /**
     * Calendar instance used to manage and display the current month and year in the calendar view.
     */
    private Calendar calendar;
    /**
     * A map storing the status (e.g., COMPLETED, PENDING, FAILED) for each day
     * within the currently displayed month. The key is the date string (YYYY-MM-DD).
     */
    private Map<String, CalendarAdapter.DayStatus> statusMap = new HashMap<>();

    // Upcoming tasks widgets
    /**
     * View representing the first upcoming task card.
     */
    private View card1;
    /**
     * View representing the second upcoming task card.
     */
    private View card2;
    /**
     * View representing the third upcoming task card.
     */
    private View card3;
    /**
     * TextView to display the name of the first upcoming task.
     */
    private TextView firstTask;
    /**
     * TextView to display the reminder time of the first upcoming task.
     */
    private TextView firstTaskTime;
    /**
     * TextView to display the name of the second upcoming task.
     */
    private TextView secondTask;
    /**
     * TextView to display the reminder time of the second upcoming task.
     */
    private TextView secondTaskTime;
    /**
     * TextView to display the name of the third upcoming task.
     */
    private TextView thirdTask;
    /**
     * TextView to display the reminder time of the third upcoming task.
     */
    private TextView thirdTaskTime;

    /**
     * Called when the activity is first created.
     * This method initializes the activity, inflates the layout,
     * sets up all UI components, and loads initial data from Firebase.
     *
     * <p>
     * The logic flow is as follows:
     * <ol>
     *     <li>Calls the superclass's {@code onCreate} method.</li>
     *     <li>Sets the content view to {@code R.layout.home_page}.</li>
     *     <li>Initializes all UI elements by finding them by their IDs (TextViews, ProgressBar, ImageViews, RecyclerView, task cards).</li>
     *     <li>Initializes the {@link Calendar} instance to the current month and sets the day to the 1st.</li>
     *     <li>Configures the {@link RecyclerView} with a {@link GridLayoutManager} for a 7-column grid (days of the week)
     *         and sets up the {@link CalendarAdapter}.</li>
     *     <li>Attaches an {@link CalendarAdapter.OnDayClickListener} to the calendar. When a day is clicked,
     *         it formats the selected date, stores it in {@link SelectedDateManager}, and starts {@link DayPageActivity}.</li>
     *     <li>Sets up click listeners for the "Previous Month" and "Next Month" buttons
     *         to navigate the calendar and trigger {@link #updateCalendar()}.</li>
     *     <li>Calls {@link #loadStreakBar()} to display the user's streak information.</li>
     *     <li>Calls {@link #updateCalendar()} to initialize and display the current month's calendar.</li>
     *     <li>Calls {@link #loadUpcomingTasks()} to fetch and display the top upcoming tasks.</li>
     * </ol>
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}. Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        // Initialize UI components
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

        // Initialize calendar to the first day of the current month
        calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        // Setup RecyclerView for calendar
        calendarAdapter = new CalendarAdapter();
        calendarRecycler.setLayoutManager(new GridLayoutManager(this, 7)); // 7 days per week
        calendarRecycler.setAdapter(calendarAdapter);

        // Set listener for day clicks on the calendar
        calendarAdapter.setOnDayClickListener(day -> {
            // Format selected date as YYYY-MM-DD
            String date = String.format(Locale.getDefault(),
                    "%04d-%02d-%02d",
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-indexed
                    day
            );
            // Store selected date and navigate to DayPageActivity
            SelectedDateManager.setSelectedDate(date);
            startActivity(new Intent(HomeActivity.this, DayPageActivity.class));
        });

        // Setup navigation buttons for calendar
        ImageView btnPrev = findViewById(R.id.btnPrevMonth);
        ImageView btnNext = findViewById(R.id.btnNextMonth);

        btnPrev.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1); // Go to previous month
            updateCalendar();
        });

        btnNext.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1); // Go to next month
            updateCalendar();
        });

        // Load initial data
        loadStreakBar();
        updateCalendar();
        loadUpcomingTasks();
    }

    /**
     * Called when the activity will start interacting with the user.
     * This is a good place to restart any UI updates or data loading
     * that might have been paused or invalidated when the activity was not in the foreground.
     * <p>
     * The logic ensures that the streak bar, calendar, and upcoming tasks
     * are refreshed every time the user returns to {@code HomeActivity},
     * reflecting any changes made in other activities (e.g., completing tasks in {@link DayPageActivity}).
     * </p>
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadStreakBar();     // Refresh streak data
        updateCalendar();    // Refresh calendar data (including task statuses)
        loadUpcomingTasks(); // Refresh upcoming tasks
    }

    /**
     * Updates the calendar UI to display the days for the month currently
     * set in the {@link #calendar} instance.
     * <p>
     * The method performs the following actions:
     * <ol>
     *     <li>Retrieves the year and month from the {@link #calendar} instance.</li>
     *     <li>Updates the {@link #txtMonthYear} TextView with the full month name and year.</li>
     *     <li>Generates a list of day strings for the current month using {@link #buildCalendarDays(Calendar)}.</li>
     *     <li>Passes the year, month, and day list to the {@link CalendarAdapter} to update its data.</li>
     *     <li>Initiates the loading of task status colors for the displayed month from Firebase
     *         by calling {@link #loadStatusColors(int, int)}.</li>
     * </ol>
     * </p>
     */
    private void updateCalendar() {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH); // Calendar.MONTH is 0-indexed

        txtMonthYear.setText(
                calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                        + " " + year
        );

        List<String> monthDays = buildCalendarDays(calendar);
        calendarAdapter.setYearMonth(year, month);
        calendarAdapter.setDays(monthDays);

        loadStatusColors(year, month);
    }

    /**
     * Generates a list of strings representing the days of the month for the given {@link Calendar} instance.
     * This list includes empty strings at the beginning to represent padding for days before the 1st of the month,
     * ensuring the calendar grid starts on the correct weekday.
     *
     * @param cal The {@link Calendar} instance set to the specific month for which to build the days.
     * @return A {@link List} of {@link String}s, where each string is either a day number
     *         (e.g., "1", "2") or an empty string for padding.
     * @see Calendar#getActualMaximum(int)
     * @see Calendar#DAY_OF_WEEK
     */
    private List<String> buildCalendarDays(Calendar cal) {
        List<String> result = new ArrayList<>();

        // Create a clone to avoid modifying the original Calendar instance
        Calendar temp = (Calendar) cal.clone();

        int daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH); // E.g., 30 or 31
        // Get the day of the week for the 1st of the month (e.g., Sunday=1, Monday=2, ..., Saturday=7)
        int firstDayWeek = temp.get(Calendar.DAY_OF_WEEK);

        // Add empty strings for padding before the first day of the month
        // This ensures the 1st day starts on the correct weekday column in the grid
        for (int i = 1; i < firstDayWeek; i++) {
            result.add("");
        }

        // Add day numbers for the entire month
        for (int d = 1; d <= daysInMonth; d++) {
            result.add(String.valueOf(d));
        }

        return result;
    }

    /**
     * Loads the task completion statuses for each day of the specified month and year from Firebase.
     * It then updates the {@link #statusMap} and notifies the {@link CalendarAdapter} to refresh the calendar UI.
     * <p>
     * The method performs the following steps:
     * <ol>
     *     <li>Clears the existing {@link #statusMap}.</li>
     *     <li>Queries the Firebase Realtime Database for all tasks belonging to the current user.
     *         The path is {@code users/[UID]/tasks}.</li>
     *     <li>Attaches a {@link ValueEventListener} to listen for a single data snapshot.</li>
     *     <li>Upon receiving data:
     *         <ul>
     *             <li>Iterates through each date entry (e.g., "2023-10-26").</li>
     *             <li>Parses the year and month from the date key and filters for the target {@code year} and {@code month}.</li>
     *             <li>For each task within a relevant date:
     *                 <ul>
     *                     <li>Checks if the task is deleted; if so, skips it.</li>
     *                     <li>Determines if there are any pending, completed, or failed tasks for that day.</li>
     *                 </ul>
     *             </li>
     *             <li>Assigns a {@link CalendarAdapter.DayStatus} to the date based on the following priority:
     *                 <ul>
     *                     <li>{@code PENDING}: If any tasks are pending.</li>
     *                     <li>{@code COMPLETED}: If all tasks are completed and none are failed.</li>
     *                     <li>{@code FAILED}: If any tasks are failed and none are completed (or mixed but failed takes precedence).</li>
     *                     <li>{@code NONE}: Default if no relevant tasks or statuses are found.</li>
     *                 </ul>
     *             </li>
     *             <li>Adds the determined status to the {@link #statusMap} with the date string as the key.</li>
     *         </ul>
     *     </li>
     *     <li>After processing all tasks, sets the updated {@link #statusMap} to the {@link #calendarAdapter},
     *         triggering a UI refresh of the calendar grid.</li>
     * </ol>
     * </p>
     *
     * @param year The year of the month for which to load status colors.
     * @param month The 0-indexed month (0 for January, 11 for December) for which to load status colors.
     */
    private void loadStatusColors(int year, int month) {
        statusMap.clear(); // Clear previous month's statuses

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            // User not logged in, handle gracefully (e.g., redirect to login)
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dateSnap : snapshot.getChildren()) {
                            String dateKey = dateSnap.getKey(); // e.g., "2023-10-26"
                            if (dateKey == null) continue;

                            // Parse year and month from dateKey (YYYY-MM-DD)
                            int y = Integer.parseInt(dateKey.substring(0, 4));
                            int m = Integer.parseInt(dateKey.substring(5, 7)) - 1; // Adjust to 0-indexed month

                            // Only process tasks for the currently displayed month
                            if (y != year || m != month) continue;

                            boolean hasPending = false;
                            boolean hasCompleted = false;
                            boolean hasFailed = false;

                            for (DataSnapshot taskSnap : dateSnap.getChildren()) {
                                Boolean deleted = taskSnap.child("deleted").getValue(Boolean.class);
                                if (deleted != null && deleted) continue; // Skip deleted tasks

                                Boolean completed = taskSnap.child("completed").getValue(Boolean.class);
                                Boolean failed = taskSnap.child("failed").getValue(Boolean.class);

                                if (completed != null && completed) hasCompleted = true;
                                else if (failed != null && failed) hasFailed = true;
                                else hasPending = true; // Task is neither completed, failed, nor deleted, so it's pending
                            }

                            CalendarAdapter.DayStatus status = CalendarAdapter.DayStatus.NONE;

                            // Determine the overall status for the day based on task states
                            if (hasPending) {
                                status = CalendarAdapter.DayStatus.PENDING;
                            } else if (hasCompleted && !hasFailed) {
                                // All tasks are completed, and no tasks failed
                                status = CalendarAdapter.DayStatus.COMPLETED;
                            } else if (!hasCompleted && hasFailed) {
                                // All tasks are failed, and no tasks completed
                                status = CalendarAdapter.DayStatus.FAILED;
                            } else if (hasCompleted && hasFailed){
                                // Mixed state: some completed, some failed. Prioritize failed for visual indication.
                                status = CalendarAdapter.DayStatus.FAILED;
                            }

                            statusMap.put(dateKey, status);
                        }
                        // Update the adapter with the new status map to refresh the calendar display
                        calendarAdapter.setStatusMap(statusMap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle potential errors (e.g., log error, show a toast)
                        // Log.e("HomeActivity", "Failed to load status colors: " + error.getMessage());
                    }
                });
    }

    /**
     * Loads the user's current streak and streak bar progress from Firebase
     * and updates the corresponding UI elements.
     * <p>
     * The method queries the Firebase Realtime Database at {@code users/[UID]} for
     * the {@code currentStreak} (Long) and {@code streakBarPercent} (Double) values.
     * <ol>
     *     <li>Fetches the {@code currentStreak} and {@code streakBarPercent} from the user's data.</li>
     *     <li>Defaults the streak value to 0 if not found.</li>
     *     <li>Updates the {@link #streakCount} TextView with the streak value.</li>
     *     <li>Updates the {@link #progressBar} with the percentage value (converting Double to int).
     *         Defaults to 0% if the percentage is not found.</li>
     *     <li>Calls {@link #getMilestoneName(long)} to determine and set the text for the {@link #streakSubtitle} TextView.</li>
     * </ol>
     * </p>
     */
    private void loadStreakBar() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            // User not logged in, handle gracefully
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .get()
                .addOnSuccessListener(snap -> {
                    // Retrieve streak and progress percentage
                    Long streak = snap.child("currentStreak").getValue(Long.class);
                    Double percent = snap.child("streakBarPercent").getValue(Double.class);

                    long streakValue = (streak == null ? 0 : streak);

                    // Update UI
                    streakCount.setText(String.valueOf(streakValue));
                    progressBar.setProgress(percent == null ? 0 : percent.intValue());

                    // Set milestone name based on streak value
                    streakSubtitle.setText(getMilestoneName(streakValue));
                });
    }

    /**
     * Determines a descriptive milestone name based on the provided streak value.
     * This method categorizes streak counts into various achievement tiers.
     *
     * @param streak The user's current activity streak count.
     * @return A {@link String} representing the milestone name corresponding to the streak value.
     */
    private String getMilestoneName(long streak) {
        if (streak == 0)
            return "Complete tasks to achieve milestones"; // Initial state

        if (streak >= 170) return "Top Performer";
        if (streak >= 120) return "Pro";
        if (streak >= 90)  return "Expert";
        if (streak >= 60)  return "High-Achiever";
        if (streak >= 35)  return "Performer";
        if (streak >= 20)  return "Achiever";
        if (streak >= 5)   return "Learner";

        // For streaks between 1 and 4
        return "Starter";
    }

    /**
     * Loads the user's upcoming, non-completed, and non-failed tasks from Firebase
     * and displays the top three on the home screen.
     * <p>
     * The method performs the following steps:
     * <ol>
     *     <li>Gets the current user's UID.</li>
     *     <li>Queries the Firebase Realtime Database for all tasks under {@code users/[UID]/tasks}.</li>
     *     <li>Upon successful data retrieval:
     *         <ul>
     *             <li>Iterates through all date snapshots and then all task snapshots for each date.</li>
     *             <li>Filters out tasks that are marked as deleted, completed, or failed.</li>
     *             <li>Extracts the task name, deadline time, and date for remaining (upcoming) tasks.</li>
     *             <li>Creates {@link TaskPreview} objects for these tasks and adds them to a list.</li>
     *         </ul>
     *     </li>
     *     <li>Sorts the list of {@link TaskPreview} objects chronologically by date and then by time.</li>
     *     <li>Calls {@link #fillTaskCard(int, TaskPreview)} to populate the first, second, and third task cards
     *         with the sorted upcoming tasks, or hides the cards if fewer than three tasks are available.</li>
     * </ol>
     * </p>
     */
    private void loadUpcomingTasks() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            // User not logged in, handle gracefully
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("tasks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<TaskPreview> list = new ArrayList<>();

                    for (DataSnapshot daySnap : snapshot.getChildren()) {
                        String date = daySnap.getKey(); // e.g., "2023-10-26"
                        if (date == null) continue;

                        for (DataSnapshot t : daySnap.getChildren()) {
                            // Retrieve task status flags
                            Boolean deleted = t.child("deleted").getValue(Boolean.class);
                            Boolean completed = t.child("completed").getValue(Boolean.class);
                            Boolean failed = t.child("failed").getValue(Boolean.class);

                            // Filter out tasks that are deleted, completed, or failed
                            if (deleted != null && deleted) continue;
                            if (completed != null && completed) continue;
                            if (failed != null && failed) continue;

                            // Get task details for upcoming tasks
                            String name = t.child("taskName").getValue(String.class);
                            String time = t.child("taskDeadlineTime").getValue(String.class);

                            if (name == null || time == null) continue;

                            list.add(new TaskPreview(name, time, date));
                        }
                    }

                    // Sort the list of upcoming tasks by date and then by time
                    Collections.sort(list, (a, b) ->
                            (a.date + " " + a.time).compareTo(b.date + " " + b.time)
                    );

                    // Populate the three upcoming task cards
                    fillTaskCard(1, list.size() > 0 ? list.get(0) : null);
                    fillTaskCard(2, list.size() > 1 ? list.get(1) : null);
                    fillTaskCard(3, list.size() > 2 ? list.get(2) : null);
                });
    }

    /**
     * Fills a specific upcoming task card with the provided task details or hides it if no task is available.
     *
     * @param index The index of the task card to fill (1 for first, 2 for second, 3 for third).
     * @param t The {@link TaskPreview} object containing the task's name, time, and date.
     *          If {@code null}, the card will be hidden.
     * <p>
     * The method performs the following actions:
     * <ol>
     *     <li>Selects the appropriate {@link View} (card) and {@link TextView}s (task name, task time)
     *         based on the provided {@code index}.</li>
     *     <li>If {@code t} is {@code null}, the card's visibility is set to {@code View.GONE}, effectively hiding it.</li>
     *     <li>If {@code t} is not {@code null}, the card's visibility is set to {@code View.VISIBLE}.</li>
     *     <li>The task name and time {@link TextView}s are updated with the data from {@code t}.</li>
     *     <li>A click listener is attached to the card, which, when triggered, sets the selected date
     *         in {@link SelectedDateManager} to the task's date and starts {@link DayPageActivity}.</li>
     * </ol>
     * </p>
     */
    private void fillTaskCard(int index, TaskPreview t) {
        View card;
        TextView taskName, taskTime;

        // Select the correct card and TextViews based on the index
        if (index == 1) {
            card = card1; taskName = firstTask; taskTime = firstTaskTime;
        } else if (index == 2) {
            card = card2; taskName = secondTask; taskTime = secondTaskTime;
        } else { // index == 3
            card = card3; taskName = thirdTask; taskTime = thirdTaskTime;
        }

        if (t == null) {
            // Hide the card if no task is provided
            card.setVisibility(View.GONE);
            return;
        }

        // Show the card and populate it with task data
        card.setVisibility(View.VISIBLE);
        taskName.setText(t.name);
        taskTime.setText(t.time);

        // Set click listener to navigate to DayPageActivity for the task's date
        card.setOnClickListener(v -> {
            SelectedDateManager.setSelectedDate(t.date);
            startActivity(new Intent(HomeActivity.this, DayPageActivity.class));
        });
    }

    /**
     * A static nested class representing a simplified preview of a task.
     * This class is used to temporarily hold essential task information
     * (name, deadline time, and date) for display in the upcoming tasks section.
     */
    public static class TaskPreview {
        /** The name or title of the task. */
        public String name;
        /** The deadline time of the task, typically in HH:MM format. */
        public String time;
        /** The date of the task, typically in YYYY-MM-DD format. */
        public String date;

        /**
         * Constructs a new {@code TaskPreview} with the specified details.
         *
         * @param name The name of the task.
         * @param time The deadline time of the task.
         * @param date The date of the task.
         */
        public TaskPreview(String name, String time, String date) {
            this.name = name;
            this.time = time;
            this.date = date;
        }
    }

    /**
     * Handles the click event for the "Go to Today" button.
     * This method sets the {@link SelectedDateManager}'s selected date to the current day
     * and navigates the user to the {@link DayPageActivity} to view tasks for today.
     *
     * @param view The {@link View} that was clicked (the "Go to Today" button).
     */
    public void goToToday(View view) {
        Calendar now = Calendar.getInstance();
        // Format today's date and set it in the manager
        SelectedDateManager.setSelectedDate(String.format(Locale.getDefault(),
                "%04d-%02d-%02d",
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1, // Calendar.MONTH is 0-indexed
                now.get(Calendar.DAY_OF_MONTH)
        ));
        // Start DayPageActivity to show today's tasks
        startActivity(new Intent(this, DayPageActivity.class));
    }

    /**
     * Handles the click event for the "View All" button.
     * This method starts the {@link ViewAllActivity}, allowing the user to
     * see a comprehensive list of all their tasks across different dates.
     *
     * @param view The {@link View} that was clicked (the "View All" button).
     */
    public void viewAllButtonListener(View view) {
        startActivity(new Intent(this, ViewAllActivity.class));
    }

    /**
     * Handles the click event for the "Logout" button.
     * This method initiates the logout process by starting the {@link LogoutActivity}.
     * {@link LogoutActivity} is expected to handle Firebase authentication sign-out
     * and navigation to the login/onboarding screen.
     *
     * @param view The {@link View} that was clicked (the "Logout" button).
     */
    public void logoutButton(View view) {
        Intent intent = new Intent(this, LogoutActivity.class);
        startActivity(intent);
    }
}
```

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
