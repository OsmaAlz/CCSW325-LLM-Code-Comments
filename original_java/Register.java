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
