package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.colormine.banking.database.DatabaseHelper;

public class LoginSignupActivity extends AppCompatActivity {

    private Button tabLogin, tabSignup;
    private LinearLayout loginFields, signupFields;
    private Button btnLogin, btnSignup;
    private TextView linkForgotPassword;
    
    // Login Views
    private EditText etLoginEmail, etLoginPassword;
    
    // Signup Views
    private EditText etSignupName, etSignupEmail, etSignupPassword;
    
    private DatabaseHelper dbHelper;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_signup);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupListeners();
    }

    private void initViews() {
        tabLogin = findViewById(R.id.tab_login);
        tabSignup = findViewById(R.id.tab_signup);
        loginFields = findViewById(R.id.login_fields);
        signupFields = findViewById(R.id.signup_fields);
        
        btnLogin = findViewById(R.id.btn_login);
        btnSignup = findViewById(R.id.btn_signup);
        linkForgotPassword = findViewById(R.id.link_forgot_password);
        
        etLoginEmail = findViewById(R.id.et_login_email);
        etLoginPassword = findViewById(R.id.et_login_password);
        
        etSignupName = findViewById(R.id.et_signup_name);
        etSignupEmail = findViewById(R.id.et_signup_email);
        etSignupPassword = findViewById(R.id.et_signup_password);
    }

    private void setupListeners() {
        tabLogin.setOnClickListener(v -> switchTab(true));
        tabSignup.setOnClickListener(v -> switchTab(false));
        
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnSignup.setOnClickListener(v -> attemptSignup());
        
        linkForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginSignupActivity.this, ForgotPasswordActivity.class));
        });
    }

    private void switchTab(boolean toLogin) {
        isLoginMode = toLogin;
        
        if (toLogin) {
            tabLogin.setBackgroundResource(R.drawable.selector_tab);
            tabLogin.setSelected(true);
            tabLogin.setTextColor(ContextCompat.getColor(this, R.color.colorTextWhite));
            
            tabSignup.setBackgroundResource(R.drawable.selector_tab);
            tabSignup.setSelected(false);
            tabSignup.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
            
            loginFields.setVisibility(View.VISIBLE);
            signupFields.setVisibility(View.GONE);
        } else {
            tabSignup.setBackgroundResource(R.drawable.selector_tab);
            tabSignup.setSelected(true);
            tabSignup.setTextColor(ContextCompat.getColor(this, R.color.colorTextWhite));
            
            tabLogin.setBackgroundResource(R.drawable.selector_tab);
            tabLogin.setSelected(false);
            tabLogin.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
            
            loginFields.setVisibility(View.GONE);
            signupFields.setVisibility(View.VISIBLE);
        }
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
