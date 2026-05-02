package com.colormine.banking.fragments;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.CardDetailActivity;
import com.colormine.banking.MainActivity;
import com.colormine.banking.R;
import com.colormine.banking.SendMoneyActivity;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.models.Transaction;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private TextView tvBalance;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvBalance = view.findViewById(R.id.card_balance_amount);
        
        initHeader(view);
        initCard(view);
        initQuickActions(view);
        initTransactions(view);

        animateBalance(0, 8450.50f);

        return view;
    }

    private void animateBalance(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(1500);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
            tvBalance.setText(format.format(value));
        });
        animator.start();
    }

    private void initHeader(View view) {
        view.findViewById(R.id.btn_open_drawer).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });
        
        TextView tvUserName = view.findViewById(R.id.tv_user_name);
        if (tvUserName != null) tvUserName.setText("Sarah Johnson");

        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            // Future: Open notifications activity/fragment
        });
    }

    private void initCard(View view) {
        view.findViewById(R.id.home_card).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CardDetailActivity.class));
        });
    }

    private void initQuickActions(View view) {
        view.findViewById(R.id.action_send).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), SendMoneyActivity.class)));
        
        view.findViewById(R.id.action_cards).setOnClickListener(v -> 
            startActivity(new Intent(getActivity(), CardDetailActivity.class)));

        view.findViewById(R.id.action_stats).setOnClickListener(v -> {
            if (getActivity() instanceof OnTabSwitchListener) {
                ((OnTabSwitchListener) getActivity()).onTabSwitchRequested(1); // Stats tab
            }
        });
    }

    private void initTransactions(View view) {
        rvTransactions = view.findViewById(R.id.rv_transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(new Transaction("1", "Dribbble Subscription", "Today, 10:45 AM", "Subscription", 14.99, false));
        transactions.add(new Transaction("2", "Upwork Escrow", "Yesterday, 03:20 PM", "Income", 850.00, true));
        transactions.add(new Transaction("3", "Starbucks", "Sep 24, 08:30 AM", "Food & Drink", 6.50, false));

        adapter = new TransactionAdapter(transactions);
        rvTransactions.setAdapter(adapter);

        view.findViewById(R.id.btn_view_all).setOnClickListener(v -> {
            if (getActivity() instanceof OnTabSwitchListener) {
                ((OnTabSwitchListener) getActivity()).onTabSwitchRequested(2); // History tab
            }
        });
    }

    public interface OnTabSwitchListener {
        void onTabSwitchRequested(int position);
    }
}
