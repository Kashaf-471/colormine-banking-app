package com.colormine.banking;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.database.DatabaseHelper;
import com.colormine.banking.models.Transaction;
import java.util.ArrayList;
import java.util.List;

public class AdminTransactionsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_transactions);

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.rv_all_transactions);
        ImageButton btnBack = findViewById(R.id.btn_back);

        transactionList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnBack.setOnClickListener(v -> finish());

        loadAllTransactions();
    }

    private void loadAllTransactions() {
        transactionList.clear();
        Cursor cursor = dbHelper.getAllTransactions();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TXN_ID));
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_EMAIL));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TYPE));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_AMOUNT));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TITLE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DATE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CATEGORY));

                transactionList.add(new Transaction(id, title + " (" + email + ")", date, category, amount, type.equals("INCOME")));
            } while (cursor.moveToNext());
            cursor.close();
        }

        adapter = new TransactionAdapter(transactionList);
        recyclerView.setAdapter(adapter);
    }
}
