package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
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

public class MainActivity extends AppCompatActivity implements HomeFragment.OnTabSwitchListener {

    public static final String EXTRA_SELECT_TAB = "extra_select_tab";
    private ViewPager2 viewPager;
    private TabLayout bottomTabs;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
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
            int id = item.getItemId();
            if (id == R.id.nav_my_account) {
                viewPager.setCurrentItem(3);
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_security) {
                startActivity(new Intent(this, SecurityActivity.class));
            } else if (id == R.id.nav_help) {
                startActivity(new Intent(this, HelpActivity.class));
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
    }

    private void setupDrawer(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            headerView.findViewById(R.id.btn_close_drawer).setOnClickListener(v -> 
                drawerLayout.closeDrawer(GravityCompat.START));
            
            // Set user info in drawer
            TextView tvName = headerView.findViewById(R.id.drawer_user_name);
            TextView tvEmail = headerView.findViewById(R.id.drawer_user_email);
            TextView tvBalance = headerView.findViewById(R.id.drawer_balance);
            
            if (tvName != null) tvName.setText("Sarah Johnson");
            if (tvEmail != null) tvEmail.setText("sarah@example.com");
            if (tvBalance != null) tvBalance.setText("$8,450.50");
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
}
