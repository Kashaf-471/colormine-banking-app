package com.colormine.banking;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.models.Transaction;
import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> allTransactions;
    
    private Button filterAll, filterIncome, filterExpenses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        rvTransactions = findViewById(R.id.rv_transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        setupData();
        setupFilters();
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
