package com.colormine.banking;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.database.DatabaseHelper;
import com.colormine.banking.models.Transaction;
import java.util.ArrayList;
import java.util.List;

public class UserDetailsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView tvInitials, tvFullName, tvEmail, tvBalance, tvStatusText, tvEmpty;
    private View statusIndicator;
    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        dbHelper = new DatabaseHelper(this);
        initViews();

        String userEmail = getIntent().getStringExtra("user_email");
        String userName = getIntent().getStringExtra("user_name");
        double userBalance = getIntent().getDoubleExtra("user_balance", 0.0);
        String userStatus = getIntent().getStringExtra("user_status");

        displayUserDetails(userName, userEmail, userBalance, userStatus);
        loadUserTransactions(userEmail);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvInitials = findViewById(R.id.tv_initials);
        tvFullName = findViewById(R.id.tv_full_name);
        tvEmail = findViewById(R.id.tv_email);
        tvBalance = findViewById(R.id.tv_balance);
        tvStatusText = findViewById(R.id.tv_status_text);
        tvEmpty = findViewById(R.id.tv_empty_transactions);
        statusIndicator = findViewById(R.id.status_indicator);
        rvTransactions = findViewById(R.id.rv_user_transactions);

        transactionList = new ArrayList<>();
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
    }

    private void displayUserDetails(String name, String email, double balance, String status) {
        tvFullName.setText(name);
        tvEmail.setText(email);
        tvBalance.setText(String.format("$%,.2f", balance));
        tvStatusText.setText(status);

        if (name != null && !name.isEmpty()) {
            tvInitials.setText(name.substring(0, 1).toUpperCase());
        }

        if ("BLOCKED".equals(status)) {
            statusIndicator.setBackgroundResource(R.drawable.bg_dot_red);
            tvStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else {
            statusIndicator.setBackgroundResource(R.drawable.bg_dot_green);
            tvStatusText.setTextColor(getResources().getColor(R.color.colorSuccess));
        }
    }

    private void loadUserTransactions(String email) {
        transactionList.clear();
        Cursor cursor = dbHelper.getUserTransactions(email);
        
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TXN_ID));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TYPE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_AMOUNT));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY));

                transactionList.add(new Transaction(id, title, date, category, amount, type.equals("INCOME")));
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (transactionList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            adapter = new TransactionAdapter(transactionList);
            rvTransactions.setAdapter(adapter);
        }
    }
}
