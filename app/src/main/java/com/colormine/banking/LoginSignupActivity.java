package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.colormine.banking.adapters.AuthPagerAdapter;
import com.colormine.banking.fragments.SignupFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LoginSignupActivity extends AppCompatActivity implements SignupFragment.OnSignupSuccessListener {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_signup);

        tabLayout = findViewById(R.id.auth_tabs);
        viewPager = findViewById(R.id.auth_viewpager);

        AuthPagerAdapter adapter = new AuthPagerAdapter(this, this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? R.string.tab_login : R.string.tab_signup);
        }).attach();
    }

    private void attemptLogin() {
        String email = etLoginEmail.getText().toString().trim();
        String password = etLoginPassword.getText().toString().trim();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Admin check
        if (email.equals("admin") && password.equals("admin")) {
            Toast.makeText(this, "Admin Login Successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AdminActivity.class));
            finish();
            return;
        }
        
        // Use SQLite for course requirement
        if (dbHelper.checkUser(email, password)) {
            // Save user session
            SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("email", email);
            editor.apply();

            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Invalid credentials. Try admin/admin", Toast.LENGTH_SHORT).show();
        }
    }

    private void attemptSignup() {
        String name = etSignupName.getText().toString().trim();
        String email = etSignupEmail.getText().toString().trim();
        String password = etSignupPassword.getText().toString().trim();
        
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (dbHelper.registerUser(name, email, password)) {
            Toast.makeText(this, "Registration Successful! Please Login", Toast.LENGTH_SHORT).show();
            switchTab(true);
        } else {
            Toast.makeText(this, "Registration Failed or Email exists", Toast.LENGTH_SHORT).show();
        }
    }
}
