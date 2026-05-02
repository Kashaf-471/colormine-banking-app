package com.colormine.banking.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.colormine.banking.fragments.LoginFragment;
import com.colormine.banking.fragments.SignupFragment;

public class AuthPagerAdapter extends FragmentStateAdapter {

    private final SignupFragment signupFragment;

    public AuthPagerAdapter(@NonNull FragmentActivity fragmentActivity, SignupFragment.OnSignupSuccessListener signupListener) {
        super(fragmentActivity);
        signupFragment = new SignupFragment();
        signupFragment.setOnSignupSuccessListener(signupListener);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new LoginFragment();
        return signupFragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
