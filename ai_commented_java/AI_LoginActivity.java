```java
package com.example.activitystreak;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * {@code LoginActivity} is the primary entry point for user authentication within the ActivityStreak application.
 * This activity allows users to log in using their email and password, provides options to register a new account,
 * reset a forgotten password, and remembers user credentials if the "Remember Me" option is selected.
 * It integrates with Firebase Authentication for secure user management and uses {@link SharedPreferences}
 * to persist login preferences locally. Additionally, it ensures the creation of a notification channel
 * necessary for displaying task reminders.
 *
 * <p>
 * Key functionalities include:
 * <ul>
 *     <li>User interface for entering email/username and password.</li>
 *     <li>Authentication against Firebase.</li>
 *     <li>Automatic appending of a default domain if the username does not contain '@'.</li>
 *     <li>"Remember Me" functionality to pre-fill login fields on subsequent launches.</li>
 *     <li>Navigation to {@link Register} activity for new user creation.</li>
 *     <li>Navigation to {@link ForgotPasswordActivity} for password reset.</li>
 *     <li>Creation of a notification channel for task reminders on Android O and above.</li>
 * </ul>
 * </p>
 *
 * @author Your Name/Team Name (e.g., ActivityStreak Developers)
 * @version 1.0
 * @since 2023-10-26
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * A constant string representing the fallback domain to be appended to a username
     * if the user does not provide a full email address (i.e., does not contain '@').
     * This allows users to log in with just a username.
     */
    private static final String FALLBACK_DOMAIN = "@activitystreak.com";

    /**
     * {@link EditText} field for user's username or email input.
     */
    private EditText username_field;
    /**
     * {@link EditText} field for user's password input.
     */
    private EditText password_field;
    /**
     * An instance of {@link FirebaseAuth} used for authenticating users with Firebase.
     */
    private FirebaseAuth auth;
    /**
     * {@link CheckBox} for the "Remember Me" option, allowing the app to save login credentials.
     */
    private CheckBox rememberMeCheckBox;

    /**
     * The name of the SharedPreferences file used to store login preferences.
     */
    private static final String PREFS_NAME = "login_prefs";
    /**
     * Key for storing the user's email or username in SharedPreferences.
     */
    private static final String KEY_EMAIL = "email";
    /**
     * Key for storing the user's password in SharedPreferences.
     */
    private static final String KEY_PASSWORD = "password";
    /**
     * Key for storing the state of the "Remember Me" checkbox in SharedPreferences.
     */
    private static final String KEY_REMEMBER = "remember_me";

    /**
     * Called when the activity is first created.
     * This method initializes the activity, sets the content view,
     * finds and assigns UI elements, initializes Firebase authentication,
     * loads any previously saved login credentials, and creates the notification channel.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b>Note: Otherwise it is null.</b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout); // Set the layout for this activity

        // Initialize UI elements by finding them by their IDs
        username_field = findViewById(R.id.username_field);
        password_field = findViewById(R.id.password_field);
        rememberMeCheckBox = findViewById(R.id.remember_me_checkbox);

        // Get the shared instance of the FirebaseAuth object
        auth = FirebaseAuth.getInstance();
        // Load any saved login credentials from SharedPreferences
        loadSavedLogin();

        // Create the notification channel required for Android O and above
        createNotificationChannel();
    }

    /**
     * Creates a notification channel for "Task Reminders" if the Android version
     * is {@link Build.VERSION_CODES#O} (Android 8.0 Oreo) or higher.
     * This channel is configured with high importance and a description to inform the user
     * about its purpose, ensuring that notifications can be delivered effectively.
     * If the Android version is lower than Oreo, this method does nothing.
     */
    private void createNotificationChannel() {
        // Check if the device is running Android O (API 26) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Define a unique ID for the notification channel
            CharSequence name = "Task Reminders";
            String description = "Channel for task deadline reminders";
            // Set the importance level for this channel (high will make sound and pop up)
            int importance = NotificationManager.IMPORTANCE_HIGH;
            // Create the NotificationChannel object
            NotificationChannel channel = new NotificationChannel("TASK_REMINDER_CHANNEL", name, importance);
            channel.setDescription(description);

            // Get the NotificationManager service
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Handles the click event for the login button.
     * It retrieves the username/email and password from the input fields,
     * performs basic validation, formats the email (appending a fallback domain if needed),
     * and attempts to sign in the user with Firebase Authentication.
     * On successful login, it saves preferences (if "Remember Me" is checked),
     * displays a success message, navigates to {@link HomeActivity}, and finishes this activity.
     * On failure, it displays an error message.
     *
     * @param view The {@link View} that triggered this method (the login button).
     */
    public void login_button_listener(View view) {
        String userOrEmail = username_field.getText().toString().trim(); // Get username/email and trim whitespace
        String password = password_field.getText().toString(); // Get password

        // Validate if username/email or password fields are empty
        if (userOrEmail.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return; // Stop execution if fields are empty
        }

        // Format the email: if it contains '@', use it as is; otherwise, append the fallback domain.
        String email = userOrEmail.contains("@") ? userOrEmail : userOrEmail + FALLBACK_DOMAIN;

        // Attempt to sign in with email and password using Firebase Authentication
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Login successful
                FirebaseUser u = auth.getCurrentUser(); // Get the current authenticated user (can be null if signed out quickly)
                saveLoginPreferences(email, password); // Save login details if "Remember Me" is checked
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show(); // Display success message
                // Navigate to the HomeActivity
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish(); // Close LoginActivity so user cannot go back to it with back button
            } else {
                // Login failed
                // Extract error message from the exception if available, otherwise use a generic message
                String msg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); // Display error message
            }
        });
    }

    /**
     * Handles the click event for the "Register" hyperlink/button.
     * It creates an {@link Intent} to start the {@link Register} activity,
     * allowing the user to create a new account.
     *
     * @param view The {@link View} that triggered this method (the register link).
     */
    public void register_hplink_listener(View view) {
        Intent intent = new Intent(LoginActivity.this, Register.class);
        startActivity(intent); // Start the Register activity
    }

    /**
     * Handles the click event for the "Forgot Password" hyperlink/button.
     * It creates an {@link Intent} to start the {@link ForgotPasswordActivity},
     * allowing the user to initiate a password reset process.
     *
     * @param view The {@link View} that triggered this method (the forgot password link).
     */
    public void forgotpwd_hplink_listener(View view) {
        Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
        startActivity(intent); // Start the ForgotPasswordActivity
    }

    /**
     * Saves the user's login preferences (email, password, and "remember me" state)
     * into {@link SharedPreferences}.
     * If the "Remember Me" checkbox is checked, the email and password are saved.
     * If unchecked, any previously saved credentials are removed.
     *
     * @param email The email address used for login.
     * @param password The password used for login.
     */
    private void saveLoginPreferences(String email, String password) {
        // Get a SharedPreferences object named PREFS_NAME in private mode
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        // Get an editor to modify the SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();

        if (rememberMeCheckBox.isChecked()) {
            // If "Remember Me" is checked, save the state and credentials
            editor.putBoolean(KEY_REMEMBER, true);
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password); // Note: Storing password directly is generally discouraged for high security apps.
        } else {
            // If "Remember Me" is not checked, clear any saved credentials and set state to false
            editor.putBoolean(KEY_REMEMBER, false);
            editor.remove(KEY_EMAIL);
            editor.remove(KEY_PASSWORD);
        }

        editor.apply(); // Apply the changes asynchronously
    }

    /**
     * Loads saved login credentials from {@link SharedPreferences}.
     * If the "Remember Me" preference was previously set to true,
     * it retrieves the saved email and password and pre-fills them into the respective
     * input fields and checks the "Remember Me" checkbox.
     */
    private void loadSavedLogin() {
        // Get a SharedPreferences object named PREFS_NAME in private mode
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Retrieve the "remember me" state, defaulting to false if not found
        boolean remember = prefs.getBoolean(KEY_REMEMBER, false);

        if (remember) {
            // If "Remember Me" was checked, load the saved email and password
            String savedEmail = prefs.getString(KEY_EMAIL, ""); // Default to empty string if not found
            String savedPassword = prefs.getString(KEY_PASSWORD, ""); // Default to empty string if not found

            // Set the loaded credentials to the UI fields
            username_field.setText(savedEmail);
            password_field.setText(savedPassword);
            rememberMeCheckBox.setChecked(true); // Check the "Remember Me" checkbox
        }
    }
}
```

package com.example.activitystreak;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String FALLBACK_DOMAIN = "@activitystreak.com";

    private EditText username_field;
    private EditText password_field;
    private FirebaseAuth auth;
    private CheckBox rememberMeCheckBox;

    private static final String PREFS_NAME = "login_prefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember_me";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        username_field = findViewById(R.id.username_field);
        password_field = findViewById(R.id.password_field);
        rememberMeCheckBox = findViewById(R.id.remember_me_checkbox);

        auth = FirebaseAuth.getInstance();
        loadSavedLogin();

        createNotificationChannel();

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "TASK_REMINDER_CHANNEL",
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for task deadline reminders");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    public void login_button_listener(View view){
        String userOrEmail = username_field.getText().toString().trim();
        String password = password_field.getText().toString();

        if (userOrEmail.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = userOrEmail.contains("@") ? userOrEmail : userOrEmail + FALLBACK_DOMAIN;

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser u = auth.getCurrentUser();
                saveLoginPreferences(email, password);
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                finish();
            } else {
                String msg = task.getException() != null ? task.getException().getMessage() : "Login failed";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void register_hplink_listener(View view){
        Intent intent = new Intent(LoginActivity.this, Register.class);
        startActivity(intent);
    }

    public void forgotpwd_hplink_listener(View view){
        Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
        startActivity(intent);
    }

    private void saveLoginPreferences(String email, String password) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (rememberMeCheckBox.isChecked()) {
            editor.putBoolean(KEY_REMEMBER, true);
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
        } else {
            editor.putBoolean(KEY_REMEMBER, false);
            editor.remove(KEY_EMAIL);
            editor.remove(KEY_PASSWORD);
        }

        editor.apply();
    }

    private void loadSavedLogin() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean remember = prefs.getBoolean(KEY_REMEMBER, false);

        if (remember) {
            String savedEmail = prefs.getString(KEY_EMAIL, "");
            String savedPassword = prefs.getString(KEY_PASSWORD, "");

            username_field.setText(savedEmail);
            password_field.setText(savedPassword);
            rememberMeCheckBox.setChecked(true);
        }
    }


}