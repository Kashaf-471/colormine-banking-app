package com.colormine.banking.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.R;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.models.Transaction;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> allTransactions;
    private TabLayout filterTabs;
    private DatabaseReference mDatabase;
    private String userEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        rvTransactions = view.findViewById(R.id.rv_transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        filterTabs = view.findViewById(R.id.filter_tabs);

        allTransactions = new ArrayList<>();
        setupTabs();
        loadTransactionsFromFirebase();

        return view;
    }

    private void loadTransactionsFromFirebase() {
        if (userEmail.isEmpty()) return;

        mDatabase.child("transactions").orderByChild("user_email").equalTo(userEmail)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    allTransactions.clear();
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        String id = postSnapshot.getKey();
                        String title = postSnapshot.child("title").getValue(String.class);
                        String date = postSnapshot.child("date").getValue(String.class);
                        String category = postSnapshot.child("category").getValue(String.class);
                        Double amount = postSnapshot.child("amount").getValue(Double.class);
                        String type = postSnapshot.child("type").getValue(String.class);

                        if (amount != null) {
                            allTransactions.add(0, new Transaction(id, title, date, category, amount, "INCOME".equals(type)));
                        }
                    }
                    
                    applyFilter(filterTabs.getSelectedTabPosition());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Failed to load history", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void setupTabs() {
        // Clear existing tabs to avoid duplication on reload
        filterTabs.removeAllTabs();
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
        
        if (adapter == null) {
            adapter = new TransactionAdapter(filtered);
            rvTransactions.setAdapter(adapter);
        } else {
            adapter.updateData(filtered);
        }
    }
}
