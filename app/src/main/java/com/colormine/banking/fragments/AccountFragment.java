package com.colormine.banking.fragments;

import android.animation.ValueAnimator;
import android.content.Intent;
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
import com.colormine.banking.R;
import com.colormine.banking.SecurityActivity;
import com.colormine.banking.SettingsActivity;

public class AccountFragment extends Fragment {

    private TextView tvStatCards, tvStatTransactions, tvStatYears;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        tvStatCards = view.findViewById(R.id.tv_stat_cards);
        tvStatTransactions = view.findViewById(R.id.tv_stat_transactions);
        tvStatYears = view.findViewById(R.id.tv_stat_years);

        // Animate stats
        animateValue(0, 12, tvStatCards, "");
        animateValue(0, 458, tvStatTransactions, "");
        animateValue(0, 5, tvStatYears, "");

        // Set up click listeners
        view.findViewById(R.id.option_personal_info).setOnClickListener(v -> {
            // Logic for personal info
        });

        view.findViewById(R.id.option_my_cards).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), CardDetailActivity.class)));

        view.findViewById(R.id.option_notifications).setOnClickListener(v -> {
            // Logic for notifications
        });

        view.findViewById(R.id.option_security).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), SecurityActivity.class)));

        view.findViewById(R.id.option_settings).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), SettingsActivity.class)));

        view.findViewById(R.id.option_help).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), HelpActivity.class)));

        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginSignupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }

    private void animateValue(int start, int end, TextView textView, String suffix) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(1200);
        animator.addUpdateListener(animation -> textView.setText(animation.getAnimatedValue().toString() + suffix));
        animator.start();
    }
}
