package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.colormine.banking.fragments.AccountFragment;
import com.colormine.banking.fragments.HistoryFragment;
import com.colormine.banking.fragments.HomeFragment;
import com.colormine.banking.fragments.StatsFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainActivity extends BaseActivity implements HomeFragment.OnTabSwitchListener {

    public static final String EXTRA_SELECT_TAB = "extra_select_tab";
    private ViewPager2 viewPager;
    private TabLayout bottomTabs;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        viewPager = findViewById(R.id.main_viewpager);
        bottomTabs = findViewById(R.id.bottom_tabs);

        setupDrawer(navigationView);

        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false);

        new TabLayoutMediator(bottomTabs, viewPager, (tab, position) -> {
            tab.setCustomView(createTabView(position));
        }).attach();
        
        bottomTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                playClickFeedback();
                updateTabView(tab, true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                updateTabView(tab, false);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            playClickFeedback();
            int id = item.getItemId();
            if (id == R.id.nav_my_account) {
                viewPager.setCurrentItem(3);
            } else if (id == R.id.nav_topup) {
                startActivity(new Intent(this, CardTransferActivity.class));
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
        });
        
        int selectedTab = getIntent().getIntExtra(EXTRA_SELECT_TAB, 0);
        viewPager.setCurrentItem(selectedTab, false);

        for (int i = 0; i < bottomTabs.getTabCount(); i++) {
            TabLayout.Tab tab = bottomTabs.getTabAt(i);
            if (tab != null) {
                updateTabView(tab, i == selectedTab);
            }
        }

        checkAndProcessRepayments();
    }

    private void checkAndProcessRepayments() {
        String email = getSharedPreferences("UserSession", MODE_PRIVATE).getString("email", "");
        if (email.isEmpty()) return;

        String sanitizedEmail = email.replace(".", ",");
        com.google.firebase.database.DatabaseReference db = com.google.firebase.database.FirebaseDatabase.getInstance().getReference();
        
        db.child("loan_requests").orderByChild("userId").equalTo(sanitizedEmail)
            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                    long now = System.currentTimeMillis();
                    for (com.google.firebase.database.DataSnapshot loanSnap : snapshot.getChildren()) {
                        String status = loanSnap.child("status").getValue(String.class);
                        Long repaymentTimestamp = loanSnap.child("repaymentTimestamp").getValue(Long.class);
                        
                        if ("GRANTED".equals(status) && repaymentTimestamp != null && now >= repaymentTimestamp) {
                            processLoanDeduction(loanSnap);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
            });
    }

    private void processLoanDeduction(com.google.firebase.database.DataSnapshot loanSnap) {
        String loanId = loanSnap.getKey();
        double amount = loanSnap.child("amount").getValue(Double.class);
        String cardId = loanSnap.child("cardId").getValue(String.class);
        String userId = loanSnap.child("userId").getValue(String.class);
        double interestRate = 0.1; // 10%
        double totalDeduction = amount * (1 + interestRate);

        com.google.firebase.database.DatabaseReference db = com.google.firebase.database.FirebaseDatabase.getInstance().getReference();
        
        db.child("users").child(userId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                com.colormine.banking.models.User user = snapshot.getValue(com.colormine.banking.models.User.class);
                if (user != null && user.getCards() != null) {
                    for (com.colormine.banking.models.Card card : user.getCards()) {
                        if (card.getId().equals(cardId)) {
                            card.setBalance(card.getBalance() - totalDeduction);
                            break;
                        }
                    }
                    user.syncGlobalBalance();
                    
                    // Update user and loan status
                    db.child("users").child(userId).setValue(user);
                    db.child("loan_requests").child(loanId).child("status").setValue("REPAID");
                    
                    // Create transaction
                    String txId = db.child("transactions").push().getKey();
                    Map<String, Object> tx = new HashMap<>();
                    tx.put("title", "Loan Auto-Repayment");
                    tx.put("date", new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date()));
                    tx.put("category", "Loan");
                    tx.put("amount", totalDeduction);
                    tx.put("type", "EXPENSE");
                    tx.put("user_email", userId.replace(",", "."));
                    tx.put("card_id", cardId);
                    tx.put("timestamp", System.currentTimeMillis());
                    if (txId != null) db.child("transactions").child(txId).setValue(tx);

                    // Notify user
                    String notifId = db.child("notifications").child(userId).push().getKey();
                    Map<String, Object> notif = new HashMap<>();
                    notif.put("title", "Loan Repaid");
                    notif.put("message", "Your loan of $" + String.format("%.2f", amount) + " plus 10% interest ($" + String.format("%.2f", totalDeduction) + " total) has been automatically deducted.");
                    notif.put("timestamp", System.currentTimeMillis());
                    notif.put("type", "loan_repayment");
                    if (notifId != null) db.child("notifications").child(userId).child(notifId).setValue(notif);
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void setupDrawer(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            headerView.findViewById(R.id.btn_close_drawer).setOnClickListener(v -> 
                drawerLayout.closeDrawer(GravityCompat.START));
        }
    }

    public void updateDrawerInfo(String name, String email, String balance) {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            TextView tvName = headerView.findViewById(R.id.drawer_user_name);
            TextView tvEmail = headerView.findViewById(R.id.drawer_user_email);
            TextView tvBalance = headerView.findViewById(R.id.drawer_balance);
            
            if (tvName != null) tvName.setText(name);
            if (tvEmail != null) tvEmail.setText(email);
            if (tvBalance != null) tvBalance.setText(balance);
        }
    }

    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.hasExtra(EXTRA_SELECT_TAB)) {
            int tabIndex = intent.getIntExtra(EXTRA_SELECT_TAB, 0);
            viewPager.setCurrentItem(tabIndex, true);
        }
    }

    private View createTabView(int position) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_bottom_tab, null);
        TextView text = view.findViewById(R.id.tab_text);
        ImageView icon = view.findViewById(R.id.tab_icon);

        switch (position) {
            case 0:
                text.setText(R.string.nav_home);
                icon.setImageResource(R.drawable.ic_wallet);
                break;
            case 1:
                text.setText(R.string.nav_stats);
                icon.setImageResource(R.drawable.ic_trending_up);
                break;
            case 2:
                text.setText(R.string.nav_history);
                icon.setImageResource(R.drawable.ic_history);
                break;
            case 3:
                text.setText(R.string.nav_account);
                icon.setImageResource(R.drawable.ic_user);
                break;
        }
        return view;
    }

    private void updateTabView(TabLayout.Tab tab, boolean selected) {
        View view = tab.getCustomView();
        if (view == null) return;
        
        TextView text = view.findViewById(R.id.tab_text);
        ImageView icon = view.findViewById(R.id.tab_icon);
        View bg = view.findViewById(R.id.tab_bg);

        if (selected) {
            text.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            icon.setColorFilter(ContextCompat.getColor(this, R.color.colorTextWhite));
            bg.setBackgroundResource(R.drawable.bg_nav_item_active);
        } else {
            text.setTextColor(ContextCompat.getColor(this, R.color.colorTextSecondary));
            icon.setColorFilter(ContextCompat.getColor(this, R.color.colorTextSecondary));
            bg.setBackgroundResource(R.drawable.bg_nav_item_inactive);
        }
    }

    @Override
    public void onTabSwitchRequested(int position) {
        viewPager.setCurrentItem(position);
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {
        public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return new HomeFragment();
                case 1: return new StatsFragment();
                case 2: return new HistoryFragment();
                case 3: return new AccountFragment();
                default: return new HomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
