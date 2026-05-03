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
 * SETUP REQUIRED:
 *   1. Replace SMTP_EMAIL with a Gmail address you own.
 *   2. Enable 2-Step Verification on that account.
 *   3. Go to Google Account → Security → App Passwords → generate one.
 *   4. Replace SMTP_PASSWORD with that 16-character App Password.
 *
 * FALLBACK:  If email sending fails (credentials not set / no network),
 *            the OTP is passed back via onFallback() so you can show it
 *            on-screen for testing.
 */
public class OtpService {

    private static final String TAG = "OtpService";

    // ──────────────────────────────────────────────────────────────────────────
    // ► CONFIGURE THESE TWO LINES before demo / submission
    // ──────────────────────────────────────────────────────────────────────────
    private static final String SMTP_EMAIL    = "colorminebankapp@gmail.com"; // sender Gmail
    private static final String SMTP_PASSWORD = "YOUR_APP_PASSWORD_HERE";     // 16-char App Password
    // ──────────────────────────────────────────────────────────────────────────

    private static final String PREF_NAME  = "OtpPrefs";
    private static final String KEY_OTP    = "otp_code";
    private static final String KEY_EMAIL  = "otp_email";
    private static final String KEY_EXPIRY = "otp_expiry";
    private static final long   OTP_VALID_MS = 5 * 60 * 1000L; // 5 minutes

    // ── Callback ─────────────────────────────────────────────────────────────

    public interface OtpCallback {
        /** Called on the main thread when email sent successfully. */
        void onSuccess();
        /** Called on the main thread when sending failed; show `fallbackOtp` on screen. */
        void onFallback(String fallbackOtp, String error);
    }

    // ── Generate + Send ───────────────────────────────────────────────────────

    /**
     * Generates a fresh OTP, persists it with a 5-minute expiry, and sends it
     * via Gmail SMTP on a background thread.
     *
     * @return the generated OTP string (use only for debug logging)
     */
    public static String generateAndSend(Context ctx, String recipientEmail, OtpCallback cb) {
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
                mainHandler.post(() -> cb.onFallback(otp, e.getMessage()));
            }
        }).start();

        return otp;
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    /**
     * Returns true if `enteredOtp` matches the stored code for `email` and
     * has not yet expired.  Clears the stored OTP on a successful match.
     */
    public static boolean verify(Context ctx, String enteredOtp, String email) {
        SharedPreferences p = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String saved = p.getString(KEY_OTP, "");
        String savedEmail = p.getString(KEY_EMAIL, "");
        long   expiry = p.getLong(KEY_EXPIRY, 0);

        if (System.currentTimeMillis() > expiry)     return false; // expired
        if (!savedEmail.equalsIgnoreCase(email))     return false; // wrong email
        if (!saved.equals(enteredOtp.trim()))        return false; // wrong code

        // Invalidate OTP after first successful use
        p.edit().remove(KEY_OTP).remove(KEY_EMAIL).remove(KEY_EXPIRY).apply();
        return true;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static void persist(Context ctx, String otp, String email) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_OTP, otp)
            .putString(KEY_EMAIL, email)
            .putLong(KEY_EXPIRY, System.currentTimeMillis() + OTP_VALID_MS)
            .apply();
    }

    private static void sendViaSmtp(String to, String otp) throws Exception {
        if (SMTP_PASSWORD.equals("YOUR_APP_PASSWORD_HERE")) {
            // Credentials not configured — force fallback so the caller knows
            throw new MessagingException("SMTP credentials not configured");
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
