package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class VerifyOtpActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvSubtitle, btnResend;
    private Button btnVerify;
    private EditText[] otpInputs = new EditText[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        String email = getIntent().getStringExtra("email");

        btnBack = findViewById(R.id.btn_back);
        tvSubtitle = findViewById(R.id.otp_subtitle);
        btnResend = findViewById(R.id.btn_resend);
        btnVerify = findViewById(R.id.btn_verify);

        if (email != null && !email.isEmpty()) {
            tvSubtitle.setText("We sent a 6-digit code to\n" + email);
        }

        otpInputs[0] = findViewById(R.id.otp_1);
        otpInputs[1] = findViewById(R.id.otp_2);
        otpInputs[2] = findViewById(R.id.otp_3);
        otpInputs[3] = findViewById(R.id.otp_4);
        otpInputs[4] = findViewById(R.id.otp_5);
        otpInputs[5] = findViewById(R.id.otp_6);

        setupOtpInputs();

        btnBack.setOnClickListener(v -> finish());

        btnResend.setOnClickListener(v -> {
            Toast.makeText(this, "New code sent!", Toast.LENGTH_SHORT).show();
        });

        btnVerify.setOnClickListener(v -> {
            StringBuilder otp = new StringBuilder();
            for (EditText input : otpInputs) {
                otp.append(input.getText().toString());
            }

            if (otp.length() == 6) {
                // Verify logic here
                Toast.makeText(this, "OTP Verified. Proceed to login.", Toast.LENGTH_LONG).show();
                // Navigate back to login
                Intent intent = new Intent(this, LoginSignupActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupOtpInputs() {
        for (int i = 0; i < otpInputs.length; i++) {
            final int currentIndex = i;
            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && currentIndex < otpInputs.length - 1) {
                        otpInputs[currentIndex + 1].requestFocus();
                    } else if (s.length() == 0 && currentIndex > 0) {
                        otpInputs[currentIndex - 1].requestFocus();
                    }
                }
            });
        }
    }
}
