package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnSendCode;
    private ProgressBar progressBar;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        ImageButton btnBack = findViewById(R.id.btn_back);
        etEmail = findViewById(R.id.et_fp_email);
        btnSendCode = findViewById(R.id.btn_send_code);
        progressBar = findViewById(R.id.fp_progress);
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

            checkUserAndSendOtp(email);
        });
    }

    private void checkUserAndSendOtp(String email) {
        setLoading(true);
        String sanitizedEmail = email.replace(".", ",");
        
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    sendOtp(email);
                } else {
                    setLoading(false);
                    Toast.makeText(ForgotPasswordActivity.this, "Email not found in our records", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(ForgotPasswordActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendOtp(String email) {
        btnSendCode.setText("Sending...");

        OtpService.generateAndSend(this, email, new OtpService.OtpCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                btnSendCode.setText("Send Code");
                Intent intent = new Intent(ForgotPasswordActivity.this, VerifyOtpActivity.class);
                intent.putExtra("email", email);
                intent.putExtra(VerifyOtpActivity.EXTRA_PURPOSE, VerifyOtpActivity.PURPOSE_PASSWORD_RESET);
                startActivity(intent);
            }

            @Override
            public void onFallback(String error) {
                setLoading(false);
                btnSendCode.setText("Send Code");
                
                // Requirement: No in-app OTP display. Strictly email.
                com.google.android.material.snackbar.Snackbar.make(btnSendCode, "📧 Email Delivery Failed. Check SMTP settings.", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getResources().getColor(R.color.colorError))
                        .setTextColor(getResources().getColor(R.color.colorTextWhite))
                        .show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSendCode.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
    }
}
