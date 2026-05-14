```java
package com.example.activitystreak;

import android.widget.CheckBox;

/**
 * Represents a single task item within the activity streak application.
 * This class serves as a data model for storing all relevant information about a task,
 * including its details, scheduling, and completion status.
 * <p>
 * It's designed to be a simple Plain Old Java Object (POJO) often used for data transfer
 * or persistence layers (e.g., with Firebase Firestore or other serialization libraries)
 * due to its public fields and a no-argument constructor.
 * </p>
 */
public class TaskItem {

    /**
     * The name or title of the task. This is typically a short, descriptive string.
     * Example: "Morning Run", "Read Chapter 5", "Meditate for 10 mins".
     */
    public String name;

    /**
     * A more detailed description of the task, providing additional context or instructions.
     * This can be null or empty if no further description is needed.
     */
    public String desc;

    /**
     * The scheduled time for the task, typically in a string format (e.g., "HH:mm", "08:00 AM").
     * This field indicates when the task is expected to be performed.
     */
    public String time;

    /**
     * The scheduled date for the task, typically in a string format (e.g., "yyyy-MM-dd", "2023-10-27").
     * This field indicates on which day the task is expected to be performed.
     */
    public String date;

    /**
     * A unique identifier for this task item. This is crucial for distinguishing tasks,
     * especially when storing and retrieving them from a database (e.g., Firebase Document ID).
     */
    public String taskId;

    /**
     * A boolean flag indicating whether the task has been successfully completed.
     * {@code true} if completed, {@code false} otherwise.
     * This uses {@link Boolean} wrapper type to allow for a potential `null` state
     * (though constructors convert `null` to `false`).
     */
    public Boolean completed;

    /**
     * A boolean flag indicating whether the task was attempted but failed, or if it was missed.
     * {@code true} if failed, {@code false} otherwise.
     * This distinguishes between "not completed yet" and "failed/missed completion".
     * This uses {@link Boolean} wrapper type to allow for a potential `null` state
     * (though constructors convert `null` to `false`).
     */
    public Boolean failed;

    /**
     * A boolean flag indicating if the task has been logically deleted.
     * This allows for "soft deletion" where the item is marked as deleted
     * but not immediately removed from the underlying data store,
     * which can be useful for recovery or auditing purposes.
     * Defaults to {@code false} when a new task is created.
     */
    public Boolean deleted;

    /**
     * A transient reference to an Android {@link CheckBox} UI component.
     * This field is marked as {@code transient} because it is a UI-specific element
     * and should not be serialized or persisted with the task data itself.
     * It's intended for temporary use when displaying tasks in a UI list.
     * It will not be included when the `TaskItem` object is converted to JSON
     * or stored in a database.
     */
    public transient CheckBox checkBox;

    /**
     * Default no-argument constructor for {@code TaskItem}.
     * This constructor is essential for many deserialization libraries (e.g., Firebase Firestore, Gson, Jackson)
     * which require a public no-arg constructor to instantiate objects when retrieving data
     * from a persistent store.
     * Fields will be initialized to their default values (null for objects, false for Booleans if not explicitly set elsewhere).
     */
    public TaskItem() {
        // Required for Firebase Firestore deserialization or similar
    }

    /**
     * Parameterized constructor for creating a new {@code TaskItem} instance with initial values.
     * This constructor allows for convenient creation of a task with its core properties
     * and initial completion/failure status.
     *
     * @param name      The name or title of the task. Cannot be null for a meaningful task.
     * @param desc      A detailed description of the task. Can be null or empty.
     * @param time      The scheduled time for the task (e.g., "HH:mm"). Can be null if time-independent.
     * @param date      The scheduled date for the task (e.g., "yyyy-MM-dd"). Cannot be null for a scheduled task.
     * @param taskId    A unique identifier for the task. Typically generated externally. Cannot be null.
     * @param completed The initial completion status. If null, defaults to {@code false}.
     * @param failed    The initial failure status. If null, defaults to {@code false}.
     */
    public TaskItem(String name, String desc, String time, String date,
                    String taskId, Boolean completed, Boolean failed) {

        this.name = name;
        this.desc = desc;
        this.time = time;
        this.date = date;
        this.taskId = taskId;

        // Logic: Ensures 'completed' is never null. If the input 'completed' is null,
        // it defaults to false. Otherwise, it uses the provided boolean value.
        this.completed = completed != null && completed;

        // Logic: Ensures 'failed' is never null. If the input 'failed' is null,
        // it defaults to false. Otherwise, it uses the provided boolean value.
        this.failed = failed != null && failed;

        // Logic: A newly created task is never deleted by default.
        // This sets the initial state for the soft-delete mechanism.
        this.deleted = false;
    }
}
```

package com.example.activitystreak;

import android.widget.CheckBox;

public class TaskItem {

    public String name;
    public String desc;
    public String time;
    public String date;
    public String taskId;

    public Boolean completed;
    public Boolean failed;
    public Boolean deleted;


    public transient CheckBox checkBox;


    public TaskItem() {
    }


    public TaskItem(String name, String desc, String time, String date,
                    String taskId, Boolean completed, Boolean failed) {

        this.name = name;
        this.desc = desc;
        this.time = time;
        this.date = date;
        this.taskId = taskId;

        this.completed = completed != null && completed;
        this.failed = failed != null && failed;

        this.deleted = false; // default
    }
}