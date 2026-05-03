package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;

public class VerifyOtpActivity extends AppCompatActivity {

    /** Pass this extra to control what happens after successful verification. */
    public static final String EXTRA_PURPOSE = "purpose";
    public static final String PURPOSE_PASSWORD_RESET = "password_reset";
    public static final String PURPOSE_SEND_MONEY     = "send_money";

    private TextView tvSubtitle, tvResend, tvCountdown;
    private Button   btnVerify;
    private ProgressBar loadingBar;
    private EditText[]  otpInputs = new EditText[6];
    private String email;
    private String purpose;
    private CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        email   = getIntent().getStringExtra("email");
        purpose = getIntent().getStringExtra(EXTRA_PURPOSE);
        if (purpose == null) purpose = PURPOSE_PASSWORD_RESET;

        ImageButton btnBack = findViewById(R.id.btn_back);
        tvSubtitle  = findViewById(R.id.otp_subtitle);
        tvResend    = findViewById(R.id.btn_resend);
        tvCountdown = findViewById(R.id.tv_countdown);
        btnVerify   = findViewById(R.id.btn_verify);
        loadingBar  = findViewById(R.id.otp_loading);

        if (loadingBar != null) loadingBar.setVisibility(View.GONE);

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
        startResendCountdown();

        btnBack.setOnClickListener(v -> finish());

        tvResend.setOnClickListener(v -> {
            if (tvResend.isEnabled()) resendOtp();
        });

        btnVerify.setOnClickListener(v -> verifyOtp());
    }

    // ── OTP input auto-advance ────────────────────────────────────────────────

    private void setupOtpInputs() {
        for (int i = 0; i < otpInputs.length; i++) {
            final int idx = i;
            otpInputs[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && idx < otpInputs.length - 1) {
                        otpInputs[idx + 1].requestFocus();
                    } else if (s.length() == 0 && idx > 0) {
                        otpInputs[idx - 1].requestFocus();
                    }
                    // Auto-verify when all 6 digits filled
                    if (allFilled()) verifyOtp();
                }
            });
        }
    }

    private boolean allFilled() {
        for (EditText e : otpInputs)
            if (e.getText().toString().isEmpty()) return false;
        return true;
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    private void verifyOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText e : otpInputs) sb.append(e.getText().toString());
        String entered = sb.toString();

        if (entered.length() < 6) {
            Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        if (OtpService.verify(this, entered, email)) {
            onVerified();
        } else {
            Toast.makeText(this, "Invalid or expired code — try again or resend", Toast.LENGTH_LONG).show();
            clearInputs();
        }
    }

    private void onVerified() {
        Toast.makeText(this, "Verified successfully!", Toast.LENGTH_SHORT).show();

        if (PURPOSE_SEND_MONEY.equals(purpose)) {
            // Return RESULT_OK so SendMoneyActivity can proceed with the transfer
            setResult(RESULT_OK);
            finish();
        } else {
            // Default: password reset flow
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra("email", email);
            startActivity(intent);
            finish();
        }
    }

    // ── Resend ────────────────────────────────────────────────────────────────

    private void resendOtp() {
        setResendEnabled(false);
        if (loadingBar != null) loadingBar.setVisibility(View.VISIBLE);

        OtpService.generateAndSend(this, email, new OtpService.OtpCallback() {
            @Override
            public void onSuccess() {
                if (loadingBar != null) loadingBar.setVisibility(View.GONE);
                Toast.makeText(VerifyOtpActivity.this,
                    "New code sent to " + email, Toast.LENGTH_SHORT).show();
                startResendCountdown();
                clearInputs();
            }

            @Override
            public void onFallback(String error) {
                if (loadingBar != null) loadingBar.setVisibility(View.GONE);
                // Requirement: No in-app OTP display. Strictly email.
                Snackbar.make(
                    btnVerify,
                    "📧 Email delivery failed. Check your internet connection.",
                    Snackbar.LENGTH_LONG
                ).show();
                startResendCountdown();
                clearInputs();
            }
        });
    }

    private void startResendCountdown() {
        if (resendTimer != null) resendTimer.cancel();
        setResendEnabled(false);

        resendTimer = new CountDownTimer(60_000, 1_000) {
            @Override
            public void onTick(long ms) {
                if (tvCountdown != null)
                    tvCountdown.setText("Resend in " + (ms / 1000) + "s");
            }
            @Override
            public void onFinish() {
                if (tvCountdown != null) tvCountdown.setText("");
                setResendEnabled(true);
            }
        }.start();
    }

    private void setResendEnabled(boolean enabled) {
        if (tvResend != null) {
            tvResend.setEnabled(enabled);
            tvResend.setAlpha(enabled ? 1f : 0.4f);
        }
    }

    private void clearInputs() {
        for (EditText e : otpInputs) e.setText("");
        otpInputs[0].requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) resendTimer.cancel();
    }
}
