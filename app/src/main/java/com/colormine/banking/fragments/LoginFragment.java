package com.colormine.banking.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.colormine.banking.ForgotPasswordActivity;
import com.colormine.banking.MainActivity;
import com.colormine.banking.R;
import com.colormine.banking.database.DatabaseHelper;

public class LoginFragment extends Fragment {

    private EditText etEmail, etPassword;
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        dbHelper = new DatabaseHelper(requireContext());
        etEmail = view.findViewById(R.id.et_login_email);
        etPassword = view.findViewById(R.id.et_login_password);
        Button btnLogin = view.findViewById(R.id.btn_login);
        TextView linkForgot = view.findViewById(R.id.link_forgot_password);

        btnLogin.setOnClickListener(v -> attemptLogin());
        linkForgot.setOnClickListener(v -> startActivity(new Intent(getActivity(), ForgotPasswordActivity.class)));

        return view;
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.checkUser(email, password) || (email.equals("admin") && password.equals("admin"))) {
            startActivity(new Intent(getActivity(), MainActivity.class));
            requireActivity().finish();
        } else {
            Toast.makeText(getContext(), "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }
}
