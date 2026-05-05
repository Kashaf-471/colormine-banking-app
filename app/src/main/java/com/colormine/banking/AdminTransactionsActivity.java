package com.colormine.banking;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

/**
 * Admin screen to view all system-wide transactions fetched from Firebase.
 */
public class AdminTransactionsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private TextView tvEmpty;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_transactions);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        recyclerView = findViewById(R.id.rv_all_transactions);
        tvEmpty = findViewById(R.id.tv_empty_transactions);
        ImageButton btnBack = findViewById(R.id.btn_back);

        transactionList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadAllTransactionsFromFirebase();
    }

    private void loadAllTransactionsFromFirebase() {
        mDatabase.child("transactions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                transactionList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    String id = postSnapshot.getKey();
                    String email = postSnapshot.child("user_email").getValue(String.class);
                    String type = postSnapshot.child("type").getValue(String.class);
                    Double amount = postSnapshot.child("amount").getValue(Double.class);
                    String title = postSnapshot.child("title").getValue(String.class);
                    String date = postSnapshot.child("date").getValue(String.class);
                    String category = postSnapshot.child("category").getValue(String.class);

                    if (amount == null) amount = 0.0;
                    
                    // Displaying email in brackets next to title for admin clarity
                    String displayTitle = title + " (" + email + ")";
                    String cardId = postSnapshot.child("card_id").getValue(String.class);
                    transactionList.add(0, new Transaction(id, displayTitle, date, category, amount, "INCOME".equals(type), cardId));
                }

                if (adapter == null) {
                    adapter = new TransactionAdapter(transactionList);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.updateData(transactionList);
                }
                
                if (tvEmpty != null) {
                    tvEmpty.setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminTransactionsActivity.this, "Failed to load transactions: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
