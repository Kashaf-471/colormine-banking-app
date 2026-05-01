package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etEmail;
    private Button btnSendCode, btnBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        btnBack = findViewById(R.id.btn_back);
        etEmail = findViewById(R.id.et_fp_email);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnBackToLogin = findViewById(R.id.btn_back_to_login);

        btnBack.setOnClickListener(v -> finish());
        
        btnBackToLogin.setOnClickListener(v -> finish());

        btnSendCode.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // In a real app, we would send an API request here
            // For now, move directly to OTP verification
            Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
            intent.putExtra("email", email);
            startActivity(intent);
        });
    }
}
