package com.colormine.banking.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.R;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.models.Transaction;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> allTransactions;
    private TabLayout filterTabs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvTransactions = view.findViewById(R.id.rv_transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        filterTabs = view.findViewById(R.id.filter_tabs);

        setupData();
        setupTabs();

        return view;
    }

    private void setupData() {
        allTransactions = new ArrayList<>();
        allTransactions.add(new Transaction("1", "Dribbble Subscription", "Today, 10:45 AM", "Subscription", 14.99, false));
        allTransactions.add(new Transaction("2", "Upwork Escrow", "Yesterday, 03:20 PM", "Income", 850.00, true));
        allTransactions.add(new Transaction("3", "Starbucks", "Sep 24, 08:30 AM", "Food & Drink", 6.50, false));
        allTransactions.add(new Transaction("4", "Amazon Prime", "Sep 22, 11:15 AM", "Shopping", 119.00, false));
        allTransactions.add(new Transaction("5", "Salary Transfer", "Sep 15, 09:00 AM", "Income", 3200.00, true));
        allTransactions.add(new Transaction("6", "Grocery Store", "Sep 14, 06:45 PM", "Groceries", 45.20, false));

        adapter = new TransactionAdapter(new ArrayList<>(allTransactions));
        rvTransactions.setAdapter(adapter);
    }

    private void setupTabs() {
        filterTabs.addTab(filterTabs.newTab().setText(R.string.filter_all));
        filterTabs.addTab(filterTabs.newTab().setText(R.string.filter_income));
        filterTabs.addTab(filterTabs.newTab().setText(R.string.filter_expenses));

        filterTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyFilter(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void applyFilter(int position) {
        List<Transaction> filtered = new ArrayList<>();
        if (position == 0) { // All
            filtered.addAll(allTransactions);
        } else if (position == 1) { // Income
            for (Transaction t : allTransactions) {
                if (t.isIncome()) filtered.add(t);
            }
        } else { // Expenses
            for (Transaction t : allTransactions) {
                if (!t.isIncome()) filtered.add(t);
            }
        }
        adapter.updateData(filtered);
    }
}
