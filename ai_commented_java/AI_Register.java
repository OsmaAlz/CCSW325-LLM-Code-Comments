```java
package com.example.activitystreak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat; // Not used in the provided code, but imported. Can be removed if not needed elsewhere.
import java.util.Date; // Not used in the provided code, but imported. Can be removed if not needed elsewhere.
import java.util.HashMap;
import java.util.Locale; // Not used in the provided code, but imported. Can be removed if not needed elsewhere.
import java.util.Map;

/**
 * The {@code Register} activity allows new users to create an account within the ActivityStreak
 * application. It handles user input for full name, username, email, and password, performs
 * client-side validation, and integrates with Firebase Authentication to register the user
 * and Firebase Realtime Database to store their initial profile information.
 *
 * <p>Users are required to provide:
 * <ul>
 *     <li>A full name</li>
 *     <li>A username</li>
 *     <li>An email address (optional, if provided, it's used; otherwise, an email is derived from the username)</li>
 *     <li>A password</li>
 *     <li>A password confirmation</li>
 * </ul>
 * </p>
 *
 * <p>After successful registration, the user's initial profile (userId, username, fullName,
 * email, currentStreak, currentMilestoneId) is saved to the Firebase Realtime Database
 * under the {@code /users/{uid}} path. Upon completion, the user is navigated to the
 * {@link LoginActivity}.</p>
 *
 * @author Your Name/Team Name (Replace with actual author)
 * @version 1.0
 * @since 2023-10-27 (Replace with actual date or version)
 */
public class Register extends AppCompatActivity {

    /**
     * A fallback domain used to construct an email address if the user does not explicitly
     * provide one, or if the provided username/email input does not contain an '@' symbol.
     * This is crucial for Firebase Authentication, which requires an email format.
     */
    private static final String FALLBACK_DOMAIN = "@activitystreak.com";

    /**
     * EditText field for the user's full name.
     */
    private EditText editFullName;
    /**
     * EditText field for the user's chosen username.
     */
    private EditText username_field;
    /**
     * EditText field for the user's password.
     */
    private EditText editPassword;
    /**
     * EditText field for confirming the user's password to ensure accuracy.
     */
    private EditText editConfirmPassword;
    /**
     * EditText field for the user's email address. This is optional; if not provided,
     * an email will be derived from the username.
     */
    private EditText editEmail;

    /**
     * An instance of {@link FirebaseAuth} used to handle user authentication (registration).
     */
    private FirebaseAuth auth;

    /**
     * Called when the activity is first created.
     * This method initializes the activity, sets the content view, and binds UI elements
     * to their respective Java objects. It also obtains an instance of {@link FirebaseAuth}.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}. Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Links the activity to its layout XML.

        // Initialize EditText fields by finding their corresponding views in the layout.
        editFullName        = findViewById(R.id.editFullName);
        username_field      = findViewById(R.id.editUsername);
        editPassword        = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        editEmail           = findViewById(R.id.editEmail);

        // Get the shared instance of the FirebaseAuth object.
        auth = FirebaseAuth.getInstance();
    }

    /**
     * This method is an event listener for the "Register" button.
     * It handles the entire user registration process, from input validation to
     * Firebase Authentication and Realtime Database storage.
     *
     * <p>The logic flow is as follows:
     * <ol>
     *     <li>Retrieve text input from all EditText fields.</li>
     *     <li>Perform client-side validation:
     *         <ul>
     *             <li>Check if all mandatory fields (name, username, password, confirm password) are filled.</li>
     *             <li>Verify that the password and confirm password fields match.</li>
     *             <li>Ensure the password is at least 6 characters long.</li>
     *         </ul>
     *     </li>
     *     <li>Construct the email address for Firebase Authentication:
     *         <ul>
     *             <li>If {@code editEmail} is provided and contains '@', use it directly.</li>
     *             <li>If {@code editEmail} is provided but lacks '@', append {@link #FALLBACK_DOMAIN}.</li>
     *             <li>If {@code editEmail} is empty, derive the email from {@code username_field}:
     *                 <ul>
     *                     <li>If username contains '@', use username directly as email.</li>
     *                     <li>Otherwise, append {@link #FALLBACK_DOMAIN} to the username.</li>
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     *     <li>Call {@link FirebaseAuth#createUserWithEmailAndPassword(String, String)} to register the user.</li>
     *     <li>Handle the result of the Firebase Authentication task:
     *         <ul>
     *             <li>If unsuccessful, display an error message (from exception or generic).</li>
     *             <li>If successful, retrieve the {@link FirebaseUser} object and its UID.</li>
     *             <li>Construct a {@link Map} representing the user's initial profile data
     *                 (userId, username, fullName, email, currentStreak, currentMilestoneId).</li>
     *             <li>Save this profile data to Firebase Realtime Database under {@code /users/{uid}}.</li>
     *             <li>Handle the result of the database save task:
     *                 <ul>
     *                     <li>If unsuccessful, display a save failure message.</li>
     *                     <li>If successful, display a "Registration successful" message,
     *                         then navigate to {@link LoginActivity} and finish the current activity.</li>
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     * </ol>
     * Any validation or Firebase operation failure results in a {@link Toast} message being displayed to the user.
     * </p>
     *
     * @param view The {@link View} that triggered this method (the "Register" button).
     */
    public void register_button_listener(View view){
        // 1. Retrieve text input from all EditText fields.
        String name       = editFullName.getText().toString().trim();
        String username   = username_field.getText().toString().trim();
        String password   = editPassword.getText().toString();
        String pwdConfirm = editConfirmPassword.getText().toString();
        String emailInput = editEmail.getText().toString().trim(); // User-provided email or empty

        // 2. Perform client-side validation.
        if (name.isEmpty() || username.isEmpty() || password.isEmpty() || pwdConfirm.isEmpty()) {
            Toast.makeText(Register.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            return; // Stop execution if validation fails.
        }

        if (!password.equals(pwdConfirm)) {
            Toast.makeText(Register.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return; // Stop execution if validation fails.
        }

        if (password.length() < 6) {
            Toast.makeText(Register.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return; // Stop execution if validation fails.
        }

        // 3. Construct the email address for Firebase Authentication.
        // Firebase Authentication requires an email format. This logic ensures one is always formed.
        String email = emailInput.isEmpty()
                ? (username.contains("@") ? username : username + FALLBACK_DOMAIN) // If emailInput is empty, derive from username
                : (emailInput.contains("@") ? emailInput : emailInput + FALLBACK_DOMAIN); // If emailInput is present, use it, appending domain if needed

        final FirebaseDatabase db = FirebaseDatabase.getInstance(); // Get Firebase Realtime Database instance.

        // 4. Call FirebaseAuth.createUserWithEmailAndPassword to register the user.
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        // 5. Handle the result of the Firebase Authentication task.
                        if (!task.isSuccessful()) {
                            // If unsuccessful, display an error message.
                            String msg = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Registration failed"; // Fallback message if no exception details.
                            Toast.makeText(Register.this, msg, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // If successful, retrieve the FirebaseUser object and its UID.
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser == null) {
                            Toast.makeText(Register.this, "Could not get user ID after registration.", Toast.LENGTH_SHORT).show();
                            // It's a rare case after successful registration, but good to handle.
                            return;
                        }

                        final String uid = firebaseUser.getUid(); // Get the unique user ID from Firebase Auth.

                        // Build initial user profile object to save to Firebase Realtime Database.
                        Map<String, Object> profile = new HashMap<>();
                        profile.put("userId", uid);              // The Firebase Authentication UID.
                        profile.put("username", username);       // User's chosen username.
                        profile.put("fullName", name);           // User's full name.
                        profile.put("email", email);             // The email used for Firebase Auth.
                        profile.put("currentStreak", 0);         // Initialize streak to 0.
                        profile.put("currentMilestoneId", null); // No milestone initially.

                        // Save this profile data to Firebase Realtime Database under /users/{uid}.
                        db.getReference("users")
                                .child(uid)
                                .setValue(profile)
                                .addOnCompleteListener(Register.this, new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(Task<Void> saveTask) {
                                        // Handle the result of the database save task.
                                        if (!saveTask.isSuccessful()) {
                                            // If unsuccessful, display a save failure message.
                                            String msg = (saveTask.getException() != null)
                                                    ? saveTask.getException().getMessage()
                                                    : "Failed to save user profile.";
                                            Toast.makeText(Register.this, msg, Toast.LENGTH_SHORT).show();
                                            // Consider rolling back auth if profile save fails consistently,
                                            // or informing the user to try logging in. For simplicity,
                                            // here it just shows an error and doesn't proceed to login.
                                            return;
                                        }

                                        // If both authentication and profile save are successful.
                                        Toast.makeText(Register.this, "Registration successful", Toast.LENGTH_SHORT).show();

                                        // Navigate to LoginActivity and finish the current Register activity.
                                        Intent intent = new Intent(Register.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish(); // Prevent user from going back to registration after successful signup.
                                    }
                                });
                    }
                });
    }

}
```

package com.example.activitystreak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Register extends AppCompatActivity {

    private static final String FALLBACK_DOMAIN = "@activitystreak.com";

    private EditText editFullName;
    private EditText username_field;
    private EditText editPassword;
    private EditText editConfirmPassword;
    private EditText editEmail;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editFullName       = findViewById(R.id.editFullName);
        username_field     = findViewById(R.id.editUsername);
        editPassword       = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        editEmail          = findViewById(R.id.editEmail);

        auth = FirebaseAuth.getInstance();
    }

    public void register_button_listener(View view){
        String name = editFullName.getText().toString().trim();
        String username = username_field.getText().toString().trim();
        String password = editPassword.getText().toString();
        String pwdConfirm = editConfirmPassword.getText().toString();
        String emailInput = editEmail.getText().toString().trim();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty() || pwdConfirm.isEmpty()) {
            Toast.makeText(Register.this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(pwdConfirm)) {
            Toast.makeText(Register.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(Register.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = emailInput.isEmpty()
                ? (username.contains("@") ? username : username + FALLBACK_DOMAIN)
                : (emailInput.contains("@") ? emailInput : emailInput + FALLBACK_DOMAIN);

        final FirebaseDatabase db = FirebaseDatabase.getInstance();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(Register.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if (!task.isSuccessful()) {
                            String msg = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Registration failed";
                            Toast.makeText(Register.this, msg, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser == null) {
                            Toast.makeText(Register.this, "Could not get user ID", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final String uid = firebaseUser.getUid();

                        // Build final user object (no dob, no createdAt)
                        Map<String, Object> profile = new HashMap<String, Object>();
                        profile.put("userId", uid);       // or custom number-based ID
                        profile.put("username", username);
                        profile.put("fullName", name);
                        profile.put("email", email);
                        profile.put("currentStreak", 0);
                        profile.put("currentMilestoneId", null);

                        // Save to /users/{uid}
                        db.getReference("users")
                                .child(uid)
                                .setValue(profile)
                                .addOnCompleteListener(Register.this, new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(Task<Void> saveTask) {
                                        if (!saveTask.isSuccessful()) {
                                            String msg = (saveTask.getException() != null)
                                                    ? saveTask.getException().getMessage()
                                                    : "Save failed";
                                            Toast.makeText(Register.this, msg, Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        Toast.makeText(Register.this, "Registration successful", Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(Register.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                    }
                });
    }

}
