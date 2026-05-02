package com.colormine.banking.adapters;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.colormine.banking.fragments.OnboardingSlideFragment;

public class OnboardingAdapter extends FragmentStateAdapter {
    
    private final String[] emojis = {"💳", "💸", "🔒"};
    private final String[] titles = {"Secure Wallet", "Fast Transfers", "Bank Grade Security"};
    private final String[] descriptions = {
        "Manage your money easily with our secure and smart wallet system.",
        "Send and receive money globally in seconds with minimal fees.",
        "Your data is protected with military-grade encryption and 2FA."
    };

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return OnboardingSlideFragment.newInstance(
            emojis[position], 
            titles[position], 
            descriptions[position]
        );
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }
}
