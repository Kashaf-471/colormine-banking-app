package com.colormine.banking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Properties;
import java.util.Random;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * OtpService — generates, sends, and verifies 6-digit OTPs via Gmail SMTP.
 * 
 * REQUIREMENT: Real-time delivery to user's email only. No in-app fallback.
 */
public class OtpService {

    private static final String TAG = "OtpService";

    // ──────────────────────────────────────────────────────────────────────────
    // ► CONFIGURE THESE TWO LINES FOR REAL-TIME EMAIL DELIVERY
    // ──────────────────────────────────────────────────────────────────────────
    private static final String SMTP_EMAIL    = "colorminebankapp@gmail.com"; 
    private static final String SMTP_PASSWORD = "dcvlshwlddhnmbju";
    // ──────────────────────────────────────────────────────────────────────────

    private static final String PREF_NAME  = "OtpPrefs";
    private static final String KEY_OTP    = "otp_code";
    private static final String KEY_EMAIL  = "otp_email";
    private static final String KEY_EXPIRY = "otp_expiry";
    private static final long   OTP_VALID_MS = 5 * 60 * 1000L; // 5 minutes

    public interface OtpCallback {
        void onSuccess();
        void onFallback(String error);
    }

    public static void generateAndSend(Context ctx, String recipientEmail, OtpCallback cb) {
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        persist(ctx, otp, recipientEmail);

        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                sendViaSmtp(recipientEmail, otp);
                Log.d(TAG, "OTP email sent to " + recipientEmail);
                mainHandler.post(cb::onSuccess);
            } catch (Exception e) {
                Log.w(TAG, "SMTP failed: " + e.getMessage());
                // Strictly pass error only, no OTP code back to UI
                mainHandler.post(() -> cb.onFallback(e.getMessage()));
            }
        }).start();
    }

    public static boolean verify(Context ctx, String enteredOtp, String email) {
        SharedPreferences p = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String saved = p.getString(KEY_OTP, "");
        String savedEmail = p.getString(KEY_EMAIL, "");
        long   expiry = p.getLong(KEY_EXPIRY, 0);

        if (System.currentTimeMillis() > expiry)     return false;
        if (!savedEmail.equalsIgnoreCase(email))     return false;
        if (!saved.equals(enteredOtp.trim()))        return false;

        p.edit().remove(KEY_OTP).remove(KEY_EMAIL).remove(KEY_EXPIRY).apply();
        return true;
    }

    private static void persist(Context ctx, String otp, String email) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_OTP, otp)
            .putString(KEY_EMAIL, email)
            .putLong(KEY_EXPIRY, System.currentTimeMillis() + OTP_VALID_MS)
            .apply();
    }

    private static void sendViaSmtp(String to, String otp) throws Exception {
        if (SMTP_PASSWORD.equals("YOUR_APP_PASSWORD_HERE") || SMTP_PASSWORD.isEmpty()) {
            throw new MessagingException("SMTP credentials are not configured in OtpService.java");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_EMAIL, SMTP_PASSWORD);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(SMTP_EMAIL, "ColorMine Bank"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject("ColorMine Bank — Your Verification Code");
        msg.setText(
            "Dear ColorMine Bank Customer,\n\n"
            + "Your one-time verification code is:\n\n"
            + "       " + otp + "\n\n"
            + "This code expires in 5 minutes.\n"
            + "Do NOT share it with anyone.\n\n"
            + "If you did not request this code, please ignore this email.\n\n"
            + "— ColorMine Bank Security Team"
        );

        Transport.send(msg);
    }
}
