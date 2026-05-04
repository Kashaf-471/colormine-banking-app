package com.colormine.banking;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.colormine.banking.adapters.AuthPagerAdapter;
import com.colormine.banking.fragments.SignupFragment;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LoginSignupActivity extends BaseActivity implements SignupFragment.OnSignupSuccessListener {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_signup);

        viewPager = findViewById(R.id.auth_viewpager);
        tabLayout = findViewById(R.id.auth_tabs);

        AuthPagerAdapter adapter = new AuthPagerAdapter(this, this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? R.string.tab_login : R.string.tab_signup);
        }).attach();
    }

    @Override
    public void onSignupSuccess() {
        if (viewPager != null) {
            viewPager.setCurrentItem(0, true);
        }
    }
}
