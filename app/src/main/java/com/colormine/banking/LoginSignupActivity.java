package com.colormine.banking;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.colormine.banking.adapters.AuthPagerAdapter;
import com.colormine.banking.fragments.SignupFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LoginSignupActivity extends AppCompatActivity implements SignupFragment.OnSignupSuccessListener {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_signup);

        tabLayout = findViewById(R.id.auth_tabs);
        viewPager = findViewById(R.id.auth_viewpager);

        AuthPagerAdapter adapter = new AuthPagerAdapter(this, this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? R.string.tab_login : R.string.tab_signup);
        }).attach();
    }

    @Override
    public void onSignupSuccess() {
        viewPager.setCurrentItem(0); // Switch to Login tab
    }
}
