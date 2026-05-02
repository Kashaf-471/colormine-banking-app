package com.colormine.banking;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private EditText etEmail;
    private Button btnSendCode, btnBackToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        
        btnBack = findViewById(R.id.btn_back);
        etEmail = findViewById(R.id.et_fp_email);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnBackToLogin = findViewById(R.id.btn_back_to_login);

        btnBack.setOnClickListener(v -> finish());
        btnBackToLogin.setOnClickListener(v -> finish());

        btnSendCode.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            
            if (email.isEmpty()) {
                etEmail.setError("Please enter your registered email");
                etEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Please enter a valid email address");
                etEmail.requestFocus();
                return;
            }

            sendResetEmail(email);
        });
    }

    private void sendResetEmail(String email) {
        btnSendCode.setEnabled(false);
        btnSendCode.setText("Sending...");

        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                btnSendCode.setEnabled(true);
                btnSendCode.setText("Send Reset Code");

                if (task.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, 
                            "A password reset link has been sent to " + email, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String message;
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthInvalidUserException e) {
                        message = "No account found with this email. Please sign up instead.";
                    } catch (Exception e) {
                        message = "Network error or invalid request. Try again later.";
                    }
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
    }
}
