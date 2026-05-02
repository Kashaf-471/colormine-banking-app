package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.models.Transaction;
import com.colormine.banking.models.User;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private DatabaseReference mDatabase;
    private String userEmail;
    private TextView tvUserName, tvBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        if (userEmail.isEmpty()) {
            startActivity(new Intent(this, LoginSignupActivity.class));
            finish();
            return;
        }

        initViews();
        setupDrawer();
        setupBottomNav();
        setupQuickActions();
        
        loadUserProfile();
        loadTransactions();
        
        // Setup Bell Icon
        RelativeLayout btnNotif = findViewById(R.id.btn_notifications);
        btnNotif.setOnClickListener(v -> {
            // Intent to NotificationsActivity
        });

        // Setup Card click
        RelativeLayout card = findViewById(R.id.home_card);
        card.setOnClickListener(v -> startActivity(new Intent(this, CardDetailActivity.class)));
        
        // Setup View All
        TextView btnViewAll = findViewById(R.id.btn_view_all);
        btnViewAll.setOnClickListener(v -> startActivity(new Intent(this, TransactionHistoryActivity.class)));
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        rvTransactions = findViewById(R.id.rv_transactions);
        tvUserName = findViewById(R.id.tv_user_name);
        tvBalance = findViewById(R.id.card_balance_amount);

        transactionList = new ArrayList<>();
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadUserProfile() {
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    String formattedBalance = String.format("$%,.2f", user.getBalance());
                    tvUserName.setText(user.getName());
                    tvBalance.setText(formattedBalance);
                    
                    // Update header if possible
                    View headerView = navigationView.getHeaderView(0);
                    if (headerView != null) {
                        ((TextView) headerView.findViewById(R.id.drawer_user_name)).setText(user.getName());
                        ((TextView) headerView.findViewById(R.id.drawer_user_email)).setText(user.getEmail());
                        ((TextView) headerView.findViewById(R.id.drawer_balance)).setText(formattedBalance);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTransactions() {
        mDatabase.child("transactions").orderByChild("user_email").equalTo(userEmail)
            .limitToLast(10)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    transactionList.clear();
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        String id = postSnapshot.getKey();
                        String title = postSnapshot.child("title").getValue(String.class);
                        String date = postSnapshot.child("date").getValue(String.class);
                        String category = postSnapshot.child("category").getValue(String.class);
                        Double amount = postSnapshot.child("amount").getValue(Double.class);
                        String type = postSnapshot.child("type").getValue(String.class);

                        if (amount != null) {
                            transactionList.add(0, new Transaction(id, title, date, category, amount, "INCOME".equals(type)));
                        }
                    }
                    if (adapter == null) {
                        adapter = new TransactionAdapter(transactionList);
                        rvTransactions.setAdapter(adapter);
                    } else {
                        adapter.updateData(transactionList);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void setupDrawer() {
        LinearLayout btnOpenDrawer = findViewById(R.id.btn_open_drawer);
        btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(this);
        
        android.view.View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            headerView.findViewById(R.id.btn_close_drawer).setOnClickListener(v -> 
                drawerLayout.closeDrawer(GravityCompat.START)
            );
        }
    }

    private void setupBottomNav() {
        findViewById(R.id.nav_stats).setOnClickListener(v -> startActivity(new Intent(this, CardStatisticActivity.class)));
        findViewById(R.id.nav_history).setOnClickListener(v -> startActivity(new Intent(this, TransactionHistoryActivity.class)));
        findViewById(R.id.nav_account).setOnClickListener(v -> startActivity(new Intent(this, AccountActivity.class)));
    }

    private void setupQuickActions() {
        findViewById(R.id.action_send).setOnClickListener(v -> startActivity(new Intent(this, SendMoneyActivity.class)));
        findViewById(R.id.action_request).setOnClickListener(v -> {
            Toast.makeText(this, "Request Money feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.action_cards).setOnClickListener(v -> startActivity(new Intent(this, CardDetailActivity.class)));
        findViewById(R.id.action_stats).setOnClickListener(v -> startActivity(new Intent(this, CardStatisticActivity.class)));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_my_account) {
            startActivity(new Intent(this, AccountActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_security) {
            startActivity(new Intent(this, SecurityActivity.class));
        } else if (id == R.id.nav_help) {
            startActivity(new Intent(this, HelpActivity.class));
        } else if (id == R.id.nav_logout) {
            getSharedPreferences("UserSession", MODE_PRIVATE).edit().clear().apply();
            startActivity(new Intent(this, LoginSignupActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
