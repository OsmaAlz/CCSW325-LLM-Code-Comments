package com.example.activitystreak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText email_field;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_layout);

        email_field = findViewById(R.id.email_field);
    }



    public void send_button_listener(View view){

        if(email_field.getText().toString().isEmpty()){
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
        }
        else{


            Intent intent = new Intent(ForgotPasswordActivity.this, ForgotPasswordActivity2.class);
            startActivity(intent);
        }


    }

    public void cancel_button_listener(View view){
        finish();
    }
}