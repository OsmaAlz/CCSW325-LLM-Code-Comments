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