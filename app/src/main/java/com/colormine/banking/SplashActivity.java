package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    
    private static final int SPLASH_DELAY = 3000; // 3 seconds delay
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Find views
        LinearLayout logoContainer = findViewById(R.id.splash_logo_container);
        TextView appName = findViewById(R.id.splash_app_name);
        TextView tagline = findViewById(R.id.splash_tagline);
        
        // Load animations
        Animation bounceAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);
        Animation slideUpAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);
        
        // Apply animations
        logoContainer.startAnimation(bounceAnim);
        appName.startAnimation(slideUpAnim);
        tagline.startAnimation(slideUpAnim);
        
        // Use Handler to navigate to Onboarding after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if user is logged in (mock logic for now, later Firebase Auth)
            boolean isLoggedIn = false;
            
            if (isLoggedIn) {
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, OnboardingActivity.class));
            }
            finish();
        }, SPLASH_DELAY);
    }
}
