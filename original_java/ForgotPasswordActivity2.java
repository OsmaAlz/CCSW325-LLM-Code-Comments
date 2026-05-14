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
