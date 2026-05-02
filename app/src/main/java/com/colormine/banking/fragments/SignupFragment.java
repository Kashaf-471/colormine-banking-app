package com.colormine.banking.fragments;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.colormine.banking.R;
import com.colormine.banking.models.User;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.Random;

public class SignupFragment extends Fragment {

    private EditText etName, etEmail, etPassword;
    private Button btnSignup;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    public interface OnSignupSuccessListener {
        void onSignupSuccess();
    }

    private OnSignupSuccessListener listener;

    public void setOnSignupSuccessListener(OnSignupSuccessListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_signup, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etName = view.findViewById(R.id.et_signup_name);
        etEmail = view.findViewById(R.id.et_signup_email);
        etPassword = view.findViewById(R.id.et_signup_password);
        btnSignup = view.findViewById(R.id.btn_signup);
        progressBar = view.findViewById(R.id.signup_progress);

        btnSignup.setOnClickListener(v -> attemptSignup());

        return view;
    }

    private void attemptSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Full name is required");
            etName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password should be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity(), task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        createNewUserInDatabase(name, email, password);
                    }
                } else {
                    setLoading(false);
                    String errorMessage;
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthWeakPasswordException e) {
                        errorMessage = "Password is too weak. Please use at least 6 characters.";
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        errorMessage = "The email address is invalid.";
                    } catch (FirebaseAuthUserCollisionException e) {
                        errorMessage = "This email is already registered. Try logging in.";
                    } catch (FirebaseNetworkException e) {
                        errorMessage = "Network error. Please check your internet connection.";
                    } catch (Exception e) {
                        errorMessage = "Signup failed: " + e.getMessage();
                    }
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void setLoading(boolean isLoading) {
        if (progressBar != null) progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (btnSignup != null) btnSignup.setEnabled(!isLoading);
        etName.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }

    private void createNewUserInDatabase(String name, String email, String password) {
        // Generate random card details
        String cardNumber = "4242 " + generateRandomDigits(4) + " " + generateRandomDigits(4) + " " + generateRandomDigits(4);
        String cardExpiry = "12/28";
        String cardCvv = generateRandomDigits(3);

        // Default balance of 5000.00, status ACTIVE, isAdmin 0
        User newUser = new User(0, name, email, password, 5000.00, "ACTIVE", 0, cardNumber, cardExpiry, cardCvv);
        
        // Firebase doesn't allow dots in keys, so we replace them
        String sanitizedEmail = email.replace(".", ",");
        
        mDatabase.child("users").child(sanitizedEmail).setValue(newUser)
            .addOnCompleteListener(task -> {
                setLoading(false);
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Registration Successful! You can now login.", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onSignupSuccess();
                    }
                } else {
                    Toast.makeText(getContext(), "Account created but profile setup failed.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private String generateRandomDigits(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
