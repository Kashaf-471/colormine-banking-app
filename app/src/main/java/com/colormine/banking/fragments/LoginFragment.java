package com.colormine.banking.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.colormine.banking.AdminActivity;
import com.colormine.banking.ForgotPasswordActivity;
import com.colormine.banking.MainActivity;
import com.colormine.banking.R;
import com.colormine.banking.models.User;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etEmail = view.findViewById(R.id.et_login_email);
        etPassword = view.findViewById(R.id.et_login_password);
        btnLogin = view.findViewById(R.id.btn_login);
        progressBar = view.findViewById(R.id.login_progress);
        TextView linkForgot = view.findViewById(R.id.link_forgot_password);

        btnLogin.setOnClickListener(v -> attemptLogin());
        linkForgot.setOnClickListener(v -> startActivity(new Intent(getActivity(), ForgotPasswordActivity.class)));

        return view;
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Please enter your email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        // Standard User Login via Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity(), task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        checkUserRoleAndStatus(user.getEmail());
                    }
                } else {
                    setLoading(false);
                    String errorMessage;
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthInvalidUserException e) {
                        errorMessage = "No account exists with this email address.";
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        errorMessage = "Incorrect password. Please verify and try again.";
                    } catch (FirebaseNetworkException e) {
                        errorMessage = "Network error. Please check your internet connection.";
                    } catch (Exception e) {
                        errorMessage = "Authentication failed. Please try again later.";
                    }
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (btnLogin != null) btnLogin.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }

    private void checkUserRoleAndStatus(String email) {
        String sanitizedEmail = email.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if ("BLOCKED".equals(user.getStatus())) {
                        mAuth.signOut();
                        Toast.makeText(getContext(), "Account Suspended: Please contact support.", Toast.LENGTH_LONG).show();
                    } else {
                        SharedPreferences pref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                        pref.edit()
                            .putString("email", email)
                            .putLong("loginTime", System.currentTimeMillis())
                            .apply();
                        
                        if (user.getIsAdmin() == 1) {
                            Toast.makeText(getContext(), "Admin Access Granted", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getActivity(), AdminActivity.class));
                        } else {
                            Toast.makeText(getContext(), "Welcome back!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getActivity(), MainActivity.class));
                        }
                        requireActivity().finish();
                    }
                } else {
                    // This case handles users authenticated in Firebase Auth but missing in Database
                    Toast.makeText(getContext(), "Profile not found. Please contact support.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
