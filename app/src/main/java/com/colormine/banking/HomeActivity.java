package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.models.Transaction;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView rvTransactions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initViews();
        setupDrawer();
        setupBottomNav();
        setupQuickActions();
        setupRecyclerView();
        
        // Setup Bell Icon
        RelativeLayout btnNotif = findViewById(R.id.btn_notifications);
        btnNotif.setOnClickListener(v -> {
            // Intent to NotificationsActivity (if requested later)
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
    }

    private void setupDrawer() {
        LinearLayout btnOpenDrawer = findViewById(R.id.btn_open_drawer);
        btnOpenDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView.setNavigationItemSelectedListener(this);
        
        // Close button in drawer header
        android.view.View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            headerView.findViewById(R.id.btn_close_drawer).setOnClickListener(v -> 
                drawerLayout.closeDrawer(GravityCompat.START)
            );
        }
    }

    private void setupBottomNav() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navStats = findViewById(R.id.nav_stats);
        LinearLayout navHistory = findViewById(R.id.nav_history);
        LinearLayout navAccount = findViewById(R.id.nav_account);

        // Home is already active
        
        navStats.setOnClickListener(v -> startActivity(new Intent(this, CardStatisticActivity.class)));
        navHistory.setOnClickListener(v -> startActivity(new Intent(this, TransactionHistoryActivity.class)));
        navAccount.setOnClickListener(v -> startActivity(new Intent(this, AccountActivity.class)));
    }

    private void setupQuickActions() {
        findViewById(R.id.action_send).setOnClickListener(v -> startActivity(new Intent(this, SendMoneyActivity.class)));
        findViewById(R.id.action_request).setOnClickListener(v -> {
            // Request money logic or activity
        });
        findViewById(R.id.action_cards).setOnClickListener(v -> startActivity(new Intent(this, CardDetailActivity.class)));
        findViewById(R.id.action_stats).setOnClickListener(v -> startActivity(new Intent(this, CardStatisticActivity.class)));
    }

    private void setupRecyclerView() {
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        
        // Dummy data based on UI design
        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction("1", "Dribbble Subscription", "Today, 10:45 AM", "Subscription", 14.99, false));
        list.add(new Transaction("2", "Upwork Escrow", "Yesterday, 03:20 PM", "Income", 850.00, true));
        list.add(new Transaction("3", "Starbucks", "Sep 24, 08:30 AM", "Food & Drink", 6.50, false));
        list.add(new Transaction("4", "Amazon Prime", "Sep 22, 11:15 AM", "Shopping", 119.00, false));
        
        TransactionAdapter adapter = new TransactionAdapter(list);
        rvTransactions.setAdapter(adapter);
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
