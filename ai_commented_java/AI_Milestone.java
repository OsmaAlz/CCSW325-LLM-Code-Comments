```java
package com.example.activitystreak;

/**
 * Represents a specific milestone or achievement within an activity tracking or streak system.
 * A {@code Milestone} defines a named goal that users can achieve by completing a certain
 * number of tasks or activities. It serves as a simple data structure (Plain Old Java Object - POJO)
 * to store the definition of an achievement, typically used to reward users or track their progress
 * towards various goals.
 *
 * <p>
 * This class encapsulates the two core attributes of a milestone:
 * <ul>
 *   <li>A descriptive {@code milestoneName} that identifies the achievement.</li>
 *   <li>The {@code tasksRequired} count, indicating how many tasks must be completed to reach this milestone.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Examples of how {@code Milestone} objects might be used:
 * <ul>
 *   <li>Define "First Streak" requiring 1 task.</li>
 *   <li>Define "Consistent User" requiring 10 tasks.</li>
 *   <li>Define "Streak Master" requiring 100 tasks.</li>
 * </ul>
 * </p>
 *
 * @author Your Name/Team Name (e.g., ActivityStreak Development Team)
 * @version 1.0
 * @since 2023-10-27
 * @see com.example.activitystreak.Activity
 * @see com.example.activitystreak.StreakTracker (hypothetical class that might use Milestones)
 */
public class Milestone {

    /**
     * The unique or descriptive name of the milestone.
     * This string identifies the achievement and is typically displayed to the user
     * or used for internal identification.
     *
     * <p>
     * **Functionality:** Stores a human-readable name for the milestone.
     * **Logic:** Should ideally be unique for each distinct milestone in a collection,
     * though uniqueness is not enforced by this class itself. It should be a
     * non-null and non-empty string.
     * </p>
     *
     * <p>
     * **Examples:** "First Task", "Daily User", "Streak Enthusiast".
     * </p>
     */
    public String milestoneName;

    /**
     * The total number of tasks or activities required to achieve this milestone.
     * A user is considered to have reached this milestone once they have completed
     * at least this many tasks within the system.
     *
     * <p>
     * **Functionality:** Defines the quantitative criterion for achieving the milestone.
     * **Logic:** This value must be non-negative (0 or greater). A typical implementation
     * would compare a user's total completed tasks against this value to determine
     * if the milestone has been met.
     * </p>
     *
     * <p>
     * **Examples:**
     * <ul>
     *   <li>1 (for a "First Task" milestone)</li>
     *   <li>10 (for a "Consistent User" milestone)</li>
     *   <li>100 (for a "Streak Master" milestone)</li>
     * </ul>
     * </p>
     */
    public long tasksRequired;

    /**
     * Default no-argument constructor for creating an empty {@code Milestone} object.
     *
     * <p>
     * **Functionality:** Initializes a {@code Milestone} object with default (null for String, 0 for long)
     * values for its fields.
     * </p>
     *
     * <p>
     * **Logic:** This constructor is primarily used by frameworks (e.g., JSON deserializers,
     * ORM frameworks like Hibernate/JPA, or dependency injection containers) that require
     * a public no-arg constructor to instantiate objects. It allows for creating an instance
     * before its properties are set, often through reflection or direct field assignment
     * by the framework.
     * </p>
     *
     * <p>
     * When using this constructor directly, ensure that {@link #milestoneName} and
     * {@link #tasksRequired} are set appropriately before the object is used to
     * represent a meaningful milestone.
     * </p>
     */
    public Milestone() {
        // No explicit initialization needed as fields will be default-initialized (null for String, 0 for long).
        // Fields will be populated by external mechanisms or subsequent setters.
    }

    /**
     * Constructs a new {@code Milestone} with the specified name and task requirement.
     *
     * <p>
     * **Functionality:** Fully initializes a {@code Milestone} object upon creation,
     * ensuring it is in a valid and complete state from the moment it is instantiated.
     * </p>
     *
     * <p>
     * **Logic:** This constructor directly assigns the provided {@code milestoneName} and
     * {@code tasksRequired} values to the corresponding fields of the new {@code Milestone} instance.
     * It promotes immutability if the fields were {@code final} (they are not in this case)
     * and ensures that a milestone object is always created with its essential data.
     * </p>
     *
     * @param milestoneName The descriptive name for this milestone.
     *                      <p>
     *                      **Parameter:** {@code milestoneName} (Type: {@code String})
     *                      **Logic:** This value is assigned to the {@link #milestoneName} field.
     *                      It should be a non-null and non-empty string that uniquely
     *                      identifies or describes the achievement.
     *                      </p>
     * @param tasksRequired The number of tasks a user must complete to achieve this milestone.
     *                      <p>
     *                      **Parameter:** {@code tasksRequired} (Type: {@code long})
     *                      **Logic:** This value is assigned to the {@link #tasksRequired} field.
     *                      It must be a non-negative number.
     *                      </p>
     */
    public Milestone(String milestoneName, long tasksRequired) {
        this.milestoneName = milestoneName;
        this.tasksRequired = tasksRequired;
    }
}
```

package com.example.activitystreak;

public class Milestone {
    public String milestoneName;
    public long tasksRequired;

    public Milestone() {}

    public Milestone(String milestoneName, long tasksRequired) {
        this.milestoneName = milestoneName;
        this.tasksRequired = tasksRequired;
    }
}