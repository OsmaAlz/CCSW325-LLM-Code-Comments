```java
package com.example.activitystreak;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * {@code ExampleUnitTest} is a basic local unit test class designed to demonstrate
 * fundamental testing principles using the JUnit framework.
 * <p>
 * This class serves as an initial example of how to write and execute unit tests
 * on the development machine (often referred to as the "host" machine, as opposed
 * to an Android device or emulator). It verifies small, isolated pieces of code
 * logic, ensuring they behave as expected.
 * </p>
 * <p>
 * While this specific example tests a trivial arithmetic operation, the principles
 * apply to more complex application logic, data processing, and utility functions
 * that do not require an Android environment to run.
 * </p>
 *
 * @author Your Name/Company (e.g., "AI Assistant")
 * @version 1.0
 * @since 2023-10-27
 * @see <a href="http://d.android.com/tools/testing">Android Testing Documentation</a>
 * @see <a href="https://junit.org/junit4/javadoc/latest/index.html">JUnit 4 API Documentation</a>
 */
public class ExampleUnitTest {

    /**
     * Tests the correctness of a basic addition operation.
     * <p>
     * This method verifies that the sum of two integers (2 + 2) correctly
     * evaluates to the expected result (4). It uses JUnit's {@code assertEquals}
     * assertion to compare the actual result of the addition with the predetermined
     * correct value.
     * </p>
     * <p>
     * This serves as a very simple sanity check and a canonical "hello world"
     * example for a unit test, demonstrating how to use the {@code @Test} annotation
     * to mark a method as a test case and how to perform a basic assertion.
     * </p>
     *
     * @implSpec
     * The test simply performs {@code 2 + 2} and asserts it equals {@code 4}.
     *
     * @see org.junit.Test
     * @see org.junit.Assert#assertEquals(long, long)
     */
    @Test
    public void addition_isCorrect() {
        // Assert that 2 + 2 equals 4.
        // If the assertion fails, the test will fail.
        assertEquals(4, 2 + 2);
    }
}
```

package com.example.activitystreak;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
}