package com.colormine.banking.fragments;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.colormine.banking.CardDetailActivity;
import com.colormine.banking.HelpActivity;
import com.colormine.banking.LoginSignupActivity;
import com.colormine.banking.NotificationsActivity;
import com.colormine.banking.R;
import com.colormine.banking.SecurityActivity;
import com.colormine.banking.SettingsActivity;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccountFragment extends Fragment {

    private TextView tvStatCards, tvStatTransactions, tvStatYears;
    private TextView tvUserName, tvUserEmail;
    private DatabaseReference mDatabase;
    private String userEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvStatCards = view.findViewById(R.id.tv_stat_cards);
        tvStatTransactions = view.findViewById(R.id.tv_stat_transactions);
        tvStatYears = view.findViewById(R.id.tv_stat_years);

        loadProfileData();
        loadStats();

        // Set up click listeners
        view.findViewById(R.id.option_personal_info).setOnClickListener(v -> {
            // Logic for personal info
        });

        view.findViewById(R.id.option_my_cards).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), CardDetailActivity.class)));

        view.findViewById(R.id.option_notifications).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), NotificationsActivity.class)));

        view.findViewById(R.id.option_security).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), SecurityActivity.class)));

        view.findViewById(R.id.option_settings).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), SettingsActivity.class)));

        view.findViewById(R.id.option_help).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), HelpActivity.class)));

        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(getActivity(), LoginSignupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }
    private void loadProfileData() {
        if (userEmail.isEmpty()) return;
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (tvUserName != null) tvUserName.setText(user.getName());
                    if (tvUserEmail != null) tvUserEmail.setText(user.getEmail());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadStats() {
        if (userEmail.isEmpty()) return;

        // Count transactions
        mDatabase.child("transactions").orderByChild("user_email").equalTo(userEmail)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long count = snapshot.getChildrenCount();
                    animateValue(0, (int) count, tvStatTransactions, "");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
            
        // Hardcoded card count to 1 for now as per logic, or 0 if none
        animateValue(0, 1, tvStatCards, "");
        // Member since years - could be calculated from registration date if stored
        animateValue(0, 1, tvStatYears, "");
    }

    private void animateValue(int start, int end, TextView textView, String suffix) {
        if (textView == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> textView.setText(animation.getAnimatedValue().toString() + suffix));
        animator.start();
    }
}
