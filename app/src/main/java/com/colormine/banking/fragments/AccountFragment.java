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
import com.colormine.banking.PersonalInfoActivity;
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

        View btnNotif = view.findViewById(R.id.btn_notifications);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> startActivity(new Intent(requireContext(), NotificationsActivity.class)));
        }

        View optPersonalInfo = view.findViewById(R.id.option_personal_info);
        if (optPersonalInfo != null) {
            optPersonalInfo.setOnClickListener(v -> startActivity(new Intent(requireContext(), PersonalInfoActivity.class)));
        }

        View optMyCards = view.findViewById(R.id.option_my_cards);
        if (optMyCards != null) {
            optMyCards.setOnClickListener(v -> startActivity(new Intent(requireContext(), CardDetailActivity.class)));
        }

        View optNotif = view.findViewById(R.id.option_notifications);
        if (optNotif != null) {
            optNotif.setOnClickListener(v -> startActivity(new Intent(requireContext(), NotificationsActivity.class)));
        }

        View optSecurity = view.findViewById(R.id.option_security);
        if (optSecurity != null) {
            optSecurity.setOnClickListener(v -> startActivity(new Intent(requireContext(), SecurityActivity.class)));
        }

        View optSettings = view.findViewById(R.id.option_settings);
        if (optSettings != null) {
            optSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));
        }

        View optHelp = view.findViewById(R.id.option_help);
        if (optHelp != null) {
            optHelp.setOnClickListener(v -> startActivity(new Intent(requireContext(), HelpActivity.class)));
        }

        Button btnLogout = view.findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE).edit().clear().apply();
                Intent intent = new Intent(requireContext(), LoginSignupActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        }

        return view;
    }
    private void loadProfileData() {
        if (userEmail.isEmpty()) return;
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
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
                    if (!isAdded()) return;
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
