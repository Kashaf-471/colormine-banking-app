package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.database.DatabaseHelper;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendCode;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        dbHelper = new DatabaseHelper(this);
        
        ImageButton btnBack = findViewById(R.id.btn_back);
        etEmail = findViewById(R.id.et_fp_email);
        btnSendCode = findViewById(R.id.btn_send_code);
        Button btnBackToLogin = findViewById(R.id.btn_back_to_login);

        btnBack.setOnClickListener(v -> finish());
        btnBackToLogin.setOnClickListener(v -> finish());

        btnSendCode.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            
            if (email.isEmpty()) {
                etEmail.setError("Please enter your registered email");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Please enter a valid email address");
                return;
            }

            // Check if user exists in local DB
            if (dbHelper.getUserName(email).equals("User") && !email.equals("sarah@example.com")) {
                Toast.makeText(this, "Email not found in our records", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSendCode.setEnabled(false);
            btnSendCode.setText("Sending...");

            OtpService.generateAndSend(this, email, new OtpService.OtpCallback() {
                @Override
                public void onSuccess() {
                    btnSendCode.setEnabled(true);
                    btnSendCode.setText("Send Code");
                    Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra(VerifyOtpActivity.EXTRA_PURPOSE, VerifyOtpActivity.PURPOSE_PASSWORD_RESET);
                    startActivity(intent);
                }

                @Override
                public void onFallback(String fallbackOtp, String error) {
                    btnSendCode.setEnabled(true);
                    btnSendCode.setText("Send Code");
                    
                    com.google.android.material.snackbar.Snackbar.make(btnSendCode, "📧 SMTP not configured. Test OTP: " + fallbackOtp, com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE)
                            .setAction("Next", v2 -> {
                                Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
                                intent.putExtra("email", email);
                                intent.putExtra(VerifyOtpActivity.EXTRA_PURPOSE, VerifyOtpActivity.PURPOSE_PASSWORD_RESET);
                                startActivity(intent);
                            }).show();
                }
            });
        });
    }
}
