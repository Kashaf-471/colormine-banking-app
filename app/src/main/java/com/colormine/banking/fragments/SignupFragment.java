package com.colormine.banking.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.colormine.banking.R;
import com.colormine.banking.TermsActivity;
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
    private CheckBox cbTerms;
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
        cbTerms = view.findViewById(R.id.cb_terms);
        btnSignup = view.findViewById(R.id.btn_signup);
        progressBar = view.findViewById(R.id.signup_progress);

        setupTermsText();
        btnSignup.setOnClickListener(v -> attemptSignup());

        return view;
    }

    private void setupTermsText() {
        String text = "I agree to the Terms and Conditions";
        SpannableString ss = new SpannableString(text);
        
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(getActivity(), TermsActivity.class));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
            }
        };

        ss.setSpan(clickableSpan, 15, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        cbTerms.setText(ss);
        cbTerms.setMovementMethod(LinkMovementMethod.getInstance());
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

        if (!cbTerms.isChecked()) {
            Toast.makeText(getContext(), "Please agree to the Terms and Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Before creating a new Firebase Auth account, check if this email was previously
        // deleted by an admin (it will still exist in Firebase Auth but not in users DB).
        String sanitizedEmailCheck = email.replace(".", ",");
        mDatabase.child("deleted_users").child(sanitizedEmailCheck)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Email was deleted by admin. Try signing in to reuse the Auth account,
                        // then overwrite the DB profile with fresh data.
                        mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(requireActivity(), signInTask -> {
                                if (signInTask.isSuccessful()) {
                                    // Remove from deleted_users and re-create the DB profile
                                    mDatabase.child("deleted_users").child(sanitizedEmailCheck).removeValue();
                                    createNewUserInDatabase(signInTask.getResult().getUser().getDisplayName() != null
                                            ? signInTask.getResult().getUser().getDisplayName() : name, email, password);
                                } else {
                                    // Old password differs — remove deleted flag and try fresh signup
                                    mDatabase.child("deleted_users").child(sanitizedEmailCheck).removeValue();
                                    proceedWithFirebaseAuthSignup(name, email, password);
                                }
                            });
                    } else {
                        // Normal path — not a deleted user
                        proceedWithFirebaseAuthSignup(name, email, password);
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    // If check fails, just proceed normally
                    proceedWithFirebaseAuthSignup(name, email, password);
                }
            });
    }

    private void proceedWithFirebaseAuthSignup(String name, String email, String password) {
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
        cbTerms.setEnabled(!isLoading);
    }

    private void createNewUserInDatabase(String name, String email, String password) {
        // Generate random card details
        String cardNumber = generateRandomDigits(4) + " " + generateRandomDigits(4) + " " + generateRandomDigits(4) + " " + generateRandomDigits(4);
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
                        recordInitialTransaction(email, sanitizedEmail);
                        listener.onSignupSuccess();
                    }
                } else {
                    Toast.makeText(getContext(), "Account created but profile setup failed.", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void recordInitialTransaction(String email, String sanitizedEmail) {
        String date = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
        String txnId = mDatabase.child("transactions").push().getKey();
        java.util.Map<String, Object> txn = new java.util.HashMap<>();
        txn.put("user_email", email);
        txn.put("type", "INCOME");
        txn.put("amount", 5000.00);
        txn.put("title", "Initial Welcome Deposit");
        txn.put("date", date);
        txn.put("category", "Deposit");
        txn.put("card_id", "default");
        if (txnId != null) mDatabase.child("transactions").child(txnId).setValue(txn);
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
