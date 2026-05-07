package com.colormine.banking;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SplashActivity extends BaseActivity {

    private static final int SPLASH_DELAY = 3000; // 3 seconds delay
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate
        android.content.SharedPreferences settings = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean darkMode = settings.getBoolean("darkMode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                darkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                        : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Find views
        LinearLayout logoContainer = findViewById(R.id.splash_logo_container);
        TextView appName = findViewById(R.id.splash_app_name);
        TextView tagline = findViewById(R.id.splash_tagline);

        // Load animations
        Animation bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);
        Animation slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);

        // Apply animations
        if (logoContainer != null)
            logoContainer.startAnimation(bounceAnim);
        if (appName != null)
            appName.startAnimation(slideUpAnim);
        if (tagline != null)
            tagline.startAnimation(slideUpAnim);

        startDecorativeAnimations();

        // Use Handler to navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            SharedPreferences sessionPref = getSharedPreferences("UserSession", MODE_PRIVATE);
            String sessionEmail = sessionPref.getString("email", "");

            if (currentUser != null && currentUser.getEmail() != null) {
                checkUserRoleAndNavigate(currentUser.getEmail());
            } else if (!sessionEmail.isEmpty()) {
                // Fallback: If Firebase Auth session is lost/sync-issue but local session exists
                checkUserRoleAndNavigate(sessionEmail);
            } else {
                // Check if it's the first time the app is launched
                SharedPreferences onboardingPref = getSharedPreferences("Onboarding", MODE_PRIVATE);
                boolean isFirstRun = onboardingPref.getBoolean("isFirstRun", true);

                if (isFirstRun) {
                    startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, LoginSignupActivity.class));
                }
                finish();
            }
        }, SPLASH_DELAY);
    }

    private void startDecorativeAnimations() {
        View blob1 = findViewById(R.id.blob_1);
        View blob2 = findViewById(R.id.blob_2);

        if (blob1 != null) {
            ObjectAnimator animX = ObjectAnimator.ofFloat(blob1, "translationX", -50f, 50f);
            animX.setDuration(4000);
            animX.setRepeatMode(ObjectAnimator.REVERSE);
            animX.setRepeatCount(ObjectAnimator.INFINITE);
            animX.start();

            ObjectAnimator animY = ObjectAnimator.ofFloat(blob1, "translationY", -30f, 30f);
            animY.setDuration(5000);
            animY.setRepeatMode(ObjectAnimator.REVERSE);
            animY.setRepeatCount(ObjectAnimator.INFINITE);
            animY.start();
        }

        if (blob2 != null) {
            ObjectAnimator animX = ObjectAnimator.ofFloat(blob2, "translationX", 30f, -30f);
            animX.setDuration(6000);
            animX.setRepeatMode(ObjectAnimator.REVERSE);
            animX.setRepeatCount(ObjectAnimator.INFINITE);
            animX.start();

            ObjectAnimator animY = ObjectAnimator.ofFloat(blob2, "translationY", 20f, -20f);
            animY.setDuration(4500);
            animY.setRepeatMode(ObjectAnimator.REVERSE);
            animY.setRepeatCount(ObjectAnimator.INFINITE);
            animY.start();
        }
    }

    private void checkUserRoleAndNavigate(String email) {
        String sanitizedEmail = email.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if ("BLOCKED".equals(user.getStatus())) {
                        mAuth.signOut();
                        startActivity(new Intent(SplashActivity.this, LoginSignupActivity.class));
                    } else {
                        SharedPreferences pref = getSharedPreferences("UserSession", MODE_PRIVATE);
                        pref.edit()
                            .putString("email", email)
                            .putInt("isAdmin", user.getIsAdmin())
                            .apply();

                        // Apply theme immediately
                        if (user.getIsAdmin() == 1) {
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                        } else {
                            boolean isDark = com.colormine.banking.utils.SettingsManager.getInstance(SplashActivity.this).isDarkMode();
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                            );
                        }
                        
                        if (user.getIsAdmin() == 1) {
                            startActivity(new Intent(SplashActivity.this, AdminActivity.class));
                        } else {
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        }
                    }
                } else {
                    // Profile missing, go to login
                    startActivity(new Intent(SplashActivity.this, LoginSignupActivity.class));
                }
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                startActivity(new Intent(SplashActivity.this, LoginSignupActivity.class));
                finish();
            }
        });
    }
}
