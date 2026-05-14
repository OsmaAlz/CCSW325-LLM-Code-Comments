```java
package com.example.activitystreak;

/**
 * Manages a single, globally accessible selected date within the application.
 * <p>
 * This utility class provides a centralized mechanism to store, retrieve, and clear
 * a specific date string that is considered "selected" across various parts of the
 * application. It uses a static field to hold the date, making it accessible
 * without needing an instance of the class. This pattern is often used for
 * managing shared application state, such as a date chosen in a calendar view
 * that needs to be referenced by other UI components or business logic.
 * </p>
 * <p>
 * The `selectedDate` is typically a string representation of a date (e.g., "YYYY-MM-DD"),
 * but the class does not enforce any particular format. It is the responsibility
 * of the caller to ensure consistency in the date string format.
 * </p>
 * <p>
 * <b>Usage Considerations:</b>
 * <ul>
 *     <li>Being a static singleton-like manager, its state is global. Be mindful
 *         of potential side effects if multiple parts of the application independently
 *         modify or read this state.</li>
 *     <li>This class is inherently <b>not thread-safe</b>. If multiple threads could
 *         potentially call {@link #setSelectedDate(String)} or {@link #clear()}
 *         concurrently, or read while another thread is writing, external
 *         synchronization mechanisms (e.g., `synchronized` blocks or `java.util.concurrent`
 *         utilities) would be required to prevent race conditions. For typical Android
 *         UI applications where UI interactions usually happen on the main thread,
 *         this might not be a direct concern, but it's important to be aware of.</li>
 *     <li>The `selectedDate` can be `null` initially or after a call to {@link #clear()}.
 *         Callers of {@link #getSelectedDate()} should handle potential `null` returns.</li>
 * </ul>
 * </p>
 *
 * @author Your Name/Team (or leave blank if unknown/not specified)
 * @version 1.0
 * @since 2023-10-27 (or specific date of creation)
 */
public class SelectedDateManager {

    /**
     * Stores the currently selected date as a String.
     * This field is static, meaning it belongs to the class itself, not to any
     * specific instance. It allows the date to be accessed and modified globally
     * across the application.
     * <p>
     * It is initialized to `null` by default and can be set to `null` explicitly
     * by the {@link #clear()} method.
     * </p>
     */
    private static String selectedDate;

    /**
     * Sets the globally selected date.
     * <p>
     * This method updates the internal static `selectedDate` field with the
     * provided date string. Any subsequent call to {@link #getSelectedDate()}
     * will return this newly set date until it is changed again or cleared.
     * </p>
     *
     * @param date The string representation of the date to be set as selected.
     *             Expected format is typically "YYYY-MM-DD" or similar, but
     *             the method does not enforce any specific format. Can be `null`.
     *             If `null` is passed, the selected date will be set to `null`.
     */
    public static void setSelectedDate(String date) {
        // Simply assigns the provided date string to the static field.
        // No format validation or complex logic is performed here,
        // as this manager primarily acts as a simple state holder.
        selectedDate = date;
    }

    /**
     * Retrieves the globally selected date.
     * <p>
     * This method returns the last date string that was set using
     * {@link #setSelectedDate(String)}, or `null` if no date has been set yet,
     * or if the date was explicitly cleared using {@link #clear()}.
     * </p>
     *
     * @return The string representation of the currently selected date,
     *         or `null` if no date is currently selected.
     */
    public static String getSelectedDate() {
        // Returns the value of the static selectedDate field.
        return selectedDate;
    }

    /**
     * Clears the globally selected date.
     * <p>
     * This method resets the internal `selectedDate` field to `null`.
     * After this method is called, {@link #getSelectedDate()} will return `null`
     * until a new date is set using {@link #setSelectedDate(String)}.
     * This is useful for resetting the application's selected date state,
     * for example, when a user navigates away from a specific view or a new
     * selection context is initiated.
     * </p>
     */
    public static void clear() {
        // Sets the static selectedDate field to null, effectively "clearing" the selection.
        selectedDate = null;
    }
}
```

package com.example.activitystreak;

public class SelectedDateManager {

    private static String selectedDate;

    public static void setSelectedDate(String date) {
        selectedDate = date;
    }

    public static String getSelectedDate() {
        return selectedDate;
    }

    public static void clear() {
        selectedDate = null;
    }
}