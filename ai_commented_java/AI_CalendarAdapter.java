```java
package com.example.activitystreak;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An adapter for displaying a monthly calendar within a {@link RecyclerView}.
 * This adapter is responsible for populating individual day cells with day numbers
 * and visually indicating the status of an activity or event for that specific day
 * (e.g., completed, pending, failed).
 * <p>
 * It extends {@link RecyclerView.Adapter} to efficiently render calendar days,
 * allowing for custom click listeners and dynamic updates of day statuses based on
 * provided data. The visual representation of a day's status is achieved by applying
 * different background colors and shapes (circular) to the day's {@link TextView}.
 * </p>
 * <p>
 * The calendar grid supports displaying empty cells for days outside the current month's
 * range (e.g., leading days from the previous month or trailing days for the next).
 * Day statuses are managed via a {@link Map} where keys are date strings in "YYYY-MM-DD"
 * format and values are {@link DayStatus} enums.
 * </p>
 *
 * @author [Your Name/Team Name]
 * @version 1.0
 * @since 2023-10-27
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    /**
     * Enumerates the possible statuses for an activity or event on a given calendar day.
     * These statuses are used to visually represent the state of an activity
     * within the calendar view, typically by applying distinct background colors.
     */
    public enum DayStatus {
        /**
         * Indicates no specific status has been assigned, or the day is empty/irrelevant.
         * This is the default state for days without activity data or for placeholder cells.
         */
        NONE,
        /**
         * Indicates an activity for this day is pending, in progress, or requires action.
         * Typically represented with a yellow background.
         */
        PENDING,
        /**
         * Indicates an activity for this day has been successfully completed.
         * Typically represented with a green background.
         */
        COMPLETED,
        /**
         * Indicates an activity for this day was attempted but failed, or a streak was broken.
         * Typically represented with a red background.
         */
        FAILED
    }

    /**
     * A list of strings representing the day numbers to be displayed in the calendar grid.
     * Empty strings ("") are used to fill leading and trailing empty cells in the calendar
     * grid, representing days from the previous or next month.
     */
    private List<String> days;
    /**
     * The year currently being displayed by this calendar adapter.
     * Used in conjunction with {@link #month} and the day number to form a unique date key
     * for looking up activity statuses.
     */
    private int year;
    /**
     * The 0-indexed month (0 for January, 11 for December) currently being displayed.
     * Used in conjunction with {@link #year} and the day number to form a unique date key.
     */
    private int month;

    /**
     * A map storing the {@link DayStatus} for specific dates.
     * The keys are date strings formatted as "YYYY-MM-DD" (e.g., "2023-10-27"),
     * and the values are the corresponding {@link DayStatus} enum.
     * This map drives the visual styling of each day cell.
     */
    private Map<String, DayStatus> statusMap = new HashMap<>();

    /**
     * Interface definition for a callback to be invoked when a calendar day is clicked.
     * Implementations of this interface can react to user interactions with individual
     * day cells.
     */
    public interface OnDayClickListener {
        /**
         * Called when a valid day (i.e., not an empty placeholder cell) in the calendar is clicked.
         *
         * @param day The integer day number (1-31) that was clicked.
         */
        void onDayClick(int day);
    }

    /**
     * The listener instance registered to receive day click events.
     * This will be null if no listener has been set.
     */
    private OnDayClickListener listener;

    /**
     * Sets the click listener for individual day cells in the calendar.
     * When a valid day cell is clicked, the {@link OnDayClickListener#onDayClick(int)}
     * method will be invoked with the corresponding day number.
     *
     * @param l The {@link OnDayClickListener} to be registered. Pass {@code null} to remove any existing listener.
     */
    public void setOnDayClickListener(OnDayClickListener l) {
        this.listener = l;
    }

    /**
     * Sets the current year and month that the adapter should consider for displaying
     * days and looking up their statuses.
     * These values are crucial for constructing the date key ("YYYY-MM-DD") used to
     * retrieve day statuses from the {@link #statusMap}.
     *
     * @param y The year (e.g., 2023).
     * @param m The 0-indexed month (e.g., 0 for January, 11 for December).
     */
    public void setYearMonth(int y, int m) {
        this.year = y;
        this.month = m;
    }

    /**
     * Sets the list of day strings to be displayed in the calendar.
     * This method expects a list containing day numbers as strings (e.g., "1", "15")
     * and potentially empty strings for placeholder cells.
     * After setting the days, {@link #notifyDataSetChanged()} is called
     * to refresh the {@link RecyclerView} and reflect the new data.
     *
     * @param d A {@link List} of {@link String} where each string represents a day number
     *          or an empty string for placeholder cells.
     */
    public void setDays(List<String> d) {
        this.days = d;
        notifyDataSetChanged();
    }

    /**
     * Sets the map containing the status for each specific date.
     * The adapter uses this map to determine the visual styling (background color)
     * of each day cell. The keys of the map should be formatted as "YYYY-MM-DD".
     * After setting the map, {@link #notifyDataSetChanged()} is called
     * to refresh the {@link RecyclerView}.
     *
     * @param map A {@link Map} where keys are date strings in "YYYY-MM-DD" format
     *            and values are {@link DayStatus} enums.
     */
    public void setStatusMap(Map<String, DayStatus> map) {
        this.statusMap = map;
        notifyDataSetChanged();
    }

    /**
     * Called when {@link RecyclerView} needs a new {@link DayViewHolder} of the given type
     * to represent an item. This method creates and initializes the {@link DayViewHolder}
     * and its associated {@link View} (by inflating `R.layout.item_day`), but does not
     * bind the view's contents to any specific data.
     *
     * @param parent The {@link ViewGroup} into which the new View will be added after it is bound to
     *               an adapter position. This is typically the {@link RecyclerView} itself.
     * @param viewType The view type of the new View. (Currently unused as there's only one item type).
     * @return A new {@link DayViewHolder} that holds a View of the given view type.
     */
    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DayViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false));
    }

    /**
     * Called by {@link RecyclerView} to display the data at the specified position.
     * This method updates the contents of the {@link DayViewHolder#itemView} to reflect
     * the item at the given {@code position} in the data set. It handles setting the
     * day number text, configuring the click listener, constructing a date key for status lookup,
     * retrieving the {@link DayStatus}, and applying the appropriate visual styling
     * (background color and shape) based on that status.
     *
     * @param holder The {@link DayViewHolder} which should be updated to represent the contents of the
     *               item at the given {@code position} in the data set.
     * @param position The position of the item within the adapter's data set (0-indexed).
     */
    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String dayText = days.get(position);

        // Logic for empty day cells (placeholders for days outside the current month)
        if (dayText.isEmpty()) {
            holder.txtDay.setText(""); // Clear any text
            holder.txtDay.setBackgroundColor(Color.TRANSPARENT); // Make background transparent
            holder.itemView.setOnClickListener(null); // Remove click listener for empty cells
            return; // No further processing needed for empty cells
        }

        // Set the day number text
        holder.txtDay.setText(dayText);

        // Parse the day number (e.g., "15" -> 15) for use in click listener and date key
        int day = Integer.parseInt(dayText);

        // Set up the click listener for the day cell's entire item view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDayClick(day); // Notify listener if set
            }
        });

        // Construct the unique date key (e.g., "2023-10-27") for status lookup.
        // The 'month' field is 0-indexed, so we add 1 for the date key format.
        String dateKey = String.format(Locale.getDefault(),
                "%04d-%02d-%02d",
                year, month + 1, day);

        // Retrieve the status for the current date from the status map.
        // If no status is explicitly found for the date, default to DayStatus.NONE.
        DayStatus status = statusMap.getOrDefault(dateKey, DayStatus.NONE);

        // Apply visual styling (background color and shape) based on the determined status.
        applyColor(holder.txtDay, status);
    }

    /**
     * Returns the total number of items (day cells) in the data set held by the adapter.
     * This includes both actual day numbers and any empty placeholder cells.
     *
     * @return The total number of items in this adapter's data set. Returns 0 if the {@link #days} list is {@code null}.
     */
    @Override
    public int getItemCount() {
        return days == null ? 0 : days.size();
    }

    /**
     * Applies a specific circular background color and sets the text color to a {@link TextView}
     * based on the given {@link DayStatus}.
     * This method utilizes {@link GradientDrawable} to programmatically create circular backgrounds
     * with distinct colors for PENDING, COMPLETED, and FAILED statuses. For {@link DayStatus#NONE},
     * a default background resource (`R.drawable.bg_day_circle`) is applied.
     *
     * @param tv The {@link TextView} representing a day cell to which the color and background should be applied.
     * @param status The {@link DayStatus} that determines the background color and shape.
     */
    private void applyColor(TextView tv, DayStatus status) {

        // Create a new GradientDrawable instance to define the custom background shape and color.
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL); // Set the shape of the drawable to an oval (which becomes a circle if width/height are equal).

        // Determine the background color based on the DayStatus using a switch statement.
        switch (status) {
            case PENDING:
                // Apply a yellow color for pending activities.
                bg.setColor(Color.parseColor("#FFEA00")); // Hex color for yellow
                break;
            case COMPLETED:
                // Apply a green color for successfully completed activities.
                bg.setColor(Color.parseColor("#A2F314")); // Hex color for green
                break;
            case FAILED:
                // Apply a red color for failed activities or broken streaks.
                bg.setColor(Color.parseColor("#FF706D")); // Hex color for red
                break;
            default:
                // For DayStatus.NONE or any unhandled status, apply a default background from resources.
                // This typically means the day is active but has no specific activity status.
                tv.setBackgroundResource(R.drawable.bg_day_circle);
                // Ensure text color is black to contrast with the default background.
                tv.setTextColor(Color.BLACK);
                return; // Exit the method early, as the background is set via resource.
        }

        // Apply the programmatically created GradientDrawable as the background for the TextView.
        tv.setBackground(bg);
        // Set the text color to black for custom-colored backgrounds to ensure readability.
        tv.setTextColor(Color.BLACK);
    }

    /**
     * A {@link RecyclerView.ViewHolder} implementation for individual day cells in the calendar grid.
     * This static nested class holds references to the UI components (specifically, the {@link TextView}
     * that displays the day number) of an item view, allowing for efficient access during view binding
     * and reducing the overhead of repeatedly calling `findViewById()`.
     */
    public static class DayViewHolder extends RecyclerView.ViewHolder {
        /**
         * The {@link TextView} displaying the day number within a calendar cell.
         */
        TextView txtDay;

        /**
         * Constructs a new {@link DayViewHolder}.
         * Initializes the view holder by finding and storing references to the UI components
         * within the provided {@code itemView}.
         *
         * @param itemView The root {@link View} of the item layout (e.g., `R.layout.item_day`)
         *                 that this ViewHolder manages.
         */
        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDay = itemView.findViewById(R.id.txtDay);
        }
    }
}
```

package com.example.activitystreak;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.DayViewHolder> {

    public enum DayStatus {
        NONE,
        PENDING,
        COMPLETED,
        FAILED
    }

    private List<String> days;
    private int year;
    private int month;

    private Map<String, DayStatus> statusMap = new HashMap<>();

    public interface OnDayClickListener {
        void onDayClick(int day);
    }

    private OnDayClickListener listener;

    public void setOnDayClickListener(OnDayClickListener l) {
        this.listener = l;
    }

    public void setYearMonth(int y, int m) {
        this.year = y;
        this.month = m;
    }

    public void setDays(List<String> d) {
        this.days = d;
        notifyDataSetChanged();
    }

    public void setStatusMap(Map<String, DayStatus> map) {
        this.statusMap = map;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DayViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String dayText = days.get(position);

        if (dayText.isEmpty()) {
            holder.txtDay.setText("");
            holder.txtDay.setBackgroundColor(Color.TRANSPARENT);
            holder.itemView.setOnClickListener(null);
            return;
        }

        holder.txtDay.setText(dayText);

        int day = Integer.parseInt(dayText);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDayClick(day);
        });

        String dateKey = String.format(Locale.getDefault(),
                "%04d-%02d-%02d",
                year, month + 1, day);

        DayStatus status = statusMap.getOrDefault(dateKey, DayStatus.NONE);
        applyColor(holder.txtDay, status);
    }

    @Override
    public int getItemCount() {
        return days == null ? 0 : days.size();
    }

    private void applyColor(TextView tv, DayStatus status) {

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);

        switch (status) {
            case PENDING:
                bg.setColor(Color.parseColor("#FFEA00"));
                break;
            case COMPLETED:
                bg.setColor(Color.parseColor("#A2F314"));
                break;
            case FAILED:
                bg.setColor(Color.parseColor("#FF706D"));
                break;
            default:
                tv.setBackgroundResource(R.drawable.bg_day_circle);
                return;
        }

        tv.setBackground(bg);
        tv.setTextColor(Color.BLACK);
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView txtDay;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDay = itemView.findViewById(R.id.txtDay);
        }
    }
}