package com.colormine.banking.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.colormine.banking.R;
import com.colormine.banking.database.DatabaseHelper;

public class SignupFragment extends Fragment {

    private EditText etName, etEmail, etPassword;
    private DatabaseHelper dbHelper;

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

        dbHelper = new DatabaseHelper(requireContext());
        etName = view.findViewById(R.id.et_signup_name);
        etEmail = view.findViewById(R.id.et_signup_email);
        etPassword = view.findViewById(R.id.et_signup_password);
        Button btnSignup = view.findViewById(R.id.btn_signup);

        btnSignup.setOnClickListener(v -> attemptSignup());

        return view;
    }

    private void attemptSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.registerUser(name, email, password)) {
            Toast.makeText(getContext(), "Registration Successful! Please Login", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onSignupSuccess();
            }
        } else {
            Toast.makeText(getContext(), "Registration Failed or Email exists", Toast.LENGTH_SHORT).show();
        }
    }
}
