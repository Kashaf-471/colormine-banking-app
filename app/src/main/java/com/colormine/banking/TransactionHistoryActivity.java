package com.colormine.banking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.models.Transaction;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> allTransactions;
    private DatabaseReference mDatabase;
    private String userEmail;
    
    private Button filterAll, filterIncome, filterExpenses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        rvTransactions = findViewById(R.id.rv_transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        allTransactions = new ArrayList<>();
        setupFilters();
        loadTransactionsFromFirebase();
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
                    
                    if (adapter == null) {
                        adapter = new TransactionAdapter(new ArrayList<>(allTransactions));
                        rvTransactions.setAdapter(adapter);
                    } else {
                        adapter.updateData(new ArrayList<>(allTransactions));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(TransactionHistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void setupFilters() {
        filterAll = findViewById(R.id.filter_all);
        filterIncome = findViewById(R.id.filter_income);
        filterExpenses = findViewById(R.id.filter_expenses);

        filterAll.setOnClickListener(v -> {
            updateFilterUI(filterAll);
            adapter.updateData(new ArrayList<>(allTransactions));
        });

        filterIncome.setOnClickListener(v -> {
            updateFilterUI(filterIncome);
            List<Transaction> filtered = new ArrayList<>();
            for (Transaction t : allTransactions) {
                if (t.isIncome()) filtered.add(t);
            }
            adapter.updateData(filtered);
        });

        filterExpenses.setOnClickListener(v -> {
            updateFilterUI(filterExpenses);
            List<Transaction> filtered = new ArrayList<>();
            for (Transaction t : allTransactions) {
                if (!t.isIncome()) filtered.add(t);
            }
            adapter.updateData(filtered);
        });
    }

    private void updateFilterUI(Button selectedBtn) {
        Button[] btns = {filterAll, filterIncome, filterExpenses};
        for (Button btn : btns) {
            if (btn == selectedBtn) {
                btn.setSelected(true);
                btn.setTextColor(ContextCompat.getColor(this, R.color.colorTextWhite));
            } else {
                btn.setSelected(false);
                btn.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
            }
        }
    }
}
