```java
package com.example.activitystreak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * {@code ForgotPasswordActivity2} is an Android Activity responsible for the second step
 * of the password reset process within the ActivityStreak application.
 * <p>
 * This activity allows users to input and confirm their new desired password after
 * potentially verifying their identity in a previous step (though that logic is not
 * present in this specific activity). It validates that the two password fields
 * are not empty and that the entered passwords match. Upon successful validation,
 * it simulates a password reset (by navigating to the {@link LoginActivity})
 * and finishes itself. If passwords do not match or are empty, appropriate
 * {@link Toast} messages are displayed to the user.
 * </p>
 * <p>
 * This activity interacts with the following UI elements from {@code activity_new_password.xml}:
 * <ul>
 *     <li>An {@link EditText} for the new password.</li>
 *     <li>An {@link EditText} for confirming the new password.</li>
 *     <li>A "Reset Password" button (handled by {@link #reset_button_listener(View)}).</li>
 *     <li>A "Back to Login" button (handled by {@link #backToLogin_button_listener(View)}).</li>
 * </ul>
 * </p>
 *
 * @author Your Name/Team Name (Replace with actual author)
 * @version 1.0
 * @since 2023-10-27 (Replace with actual creation date)
 */
public class ForgotPasswordActivity2 extends AppCompatActivity {

    /**
     * {@link EditText} UI component for users to input their new password.
     * This field is initialized in {@link #onCreate(Bundle)} by finding its
     * corresponding ID from the layout file.
     */
    private EditText editNewPassword;

    /**
     * {@link EditText} UI component for users to confirm their new password.
     * This field is initialized in {@link #onCreate(Bundle)} by finding its
     * corresponding ID from the layout file. Its value is compared against
     * {@link #editNewPassword} during the reset process.
     */
    private EditText editConfirmPassword;

    /**
     * Called when the activity is first created. This is where you should do all of your
     * normal static set up: create views, bind data to lists, etc.
     * <p>
     * In this implementation, it performs the following:
     * <ol>
     *     <li>Calls the superclass's {@code onCreate} method to maintain the activity lifecycle.</li>
     *     <li>Sets the user interface layout for this activity from {@code R.layout.activity_new_password}.</li>
     *     <li>Initializes the {@link EditText} fields {@link #editNewPassword} and
     *         {@link #editConfirmPassword} by finding them by their respective IDs
     *         ({@code R.id.editNewPassword} and {@code R.id.editConfirmPassword})
     *         from the inflated layout.</li>
     * </ol>
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState(Bundle)}.
     *     Note: Otherwise it is null.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
    }

    /**
     * This method is called when the "Reset Password" button is clicked in the UI.
     * It handles the logic for validating the new password input.
     * <p>
     * The logic proceeds as follows:
     * <ol>
     *     <li>It retrieves the text entered in {@link #editNewPassword} and {@link #editConfirmPassword}.</li>
     *     <li><b>Validation 1: Empty Fields Check</b>
     *         If either of the password fields is empty, a {@link Toast} message
     *         "Please enter your new password" is displayed, and the method
     *         returns without further action.</li>
     *     <li><b>Validation 2: Password Match Check</b>
     *         If both fields are not empty, it compares the text in
     *         {@link #editNewPassword} and {@link #editConfirmPassword}.
     *         <ul>
     *             <li>If they match:
     *                 <p>
     *                 A new {@link Intent} is created to navigate the user to the
     *                 {@link LoginActivity}. The {@link LoginActivity} is started,
     *                 this current activity ({@code ForgotPasswordActivity2}) is
     *                 finished using {@link #finish()} to remove it from the back stack,
     *                 and a {@link Toast} message "Password reset successful" is displayed.
     *                 </p>
     *             </li>
     *             <li>If they do not match:
     *                 <p>
     *                 A {@link Toast} message "Passwords do not match" is displayed,
     *                 indicating an input error to the user.
     *                 </p>
     *             </li>
     *         </ul>
     *     </li>
     * </ol>
     * </p>
     *
     * @param view The {@link View} that was clicked (typically a Button associated
     *             with this listener in the layout XML via {@code android:onClick}).
     */
    public void reset_button_listener(View view) {
        // Retrieve the text from the password input fields.
        String newPassword = editNewPassword.getText().toString();
        String confirmPassword = editConfirmPassword.getText().toString();

        // Check if either password field is empty.
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please enter your new password", Toast.LENGTH_SHORT).show();
        }
        // Check if the new password and confirm password match.
        else if (newPassword.equals(confirmPassword)) {
            // Passwords match, simulate successful reset and navigate to LoginActivity.
            Intent intent = new Intent(ForgotPasswordActivity2.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Remove this activity from the back stack.
            Toast.makeText(this, "Password reset successful", Toast.LENGTH_SHORT).show();
        }
        // Passwords do not match.
        else {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This method is called when the "Back to Login" button is clicked in the UI.
     * It provides a way for users to return to the login screen without completing
     * the password reset process.
     * <p>
     * The logic is straightforward:
     * <ol>
     *     <li>A new {@link Intent} is created to navigate the user to the
     *         {@link LoginActivity}.</li>
     *     <li>The {@link LoginActivity} is started using {@link #startActivity(Intent)}.</li>
     *     <li>This current activity ({@code ForgotPasswordActivity2}) is finished
     *         using {@link #finish()}, removing it from the activity back stack.</li>
     * </ol>
     * </p>
     *
     * @param view The {@link View} that was clicked (typically a Button associated
     *             with this listener in the layout XML via {@code android:onClick}).
     */
    public void backToLogin_button_listener(View view) {
        // Create an intent to go back to the LoginActivity.
        Intent intent = new Intent(ForgotPasswordActivity2.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Remove this activity from the back stack.
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

public class ForgotPasswordActivity2 extends AppCompatActivity{

    private EditText editNewPassword;
    private EditText editConfirmPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
    }



    public void reset_button_listener(View view){

        if(editNewPassword.getText().toString().isEmpty() || editConfirmPassword.getText().toString().isEmpty()){
            Toast.makeText(this, "Please enter your new password", Toast.LENGTH_SHORT).show();
        }
        else if(editNewPassword.getText().toString().equals(editConfirmPassword.getText().toString())){
            Intent intent = new Intent(ForgotPasswordActivity2.this, LoginActivity.class);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Password reset successful", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
        }

    }

    public void backToLogin_button_listener(View view){
        Intent intent = new Intent(ForgotPasswordActivity2.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


}
