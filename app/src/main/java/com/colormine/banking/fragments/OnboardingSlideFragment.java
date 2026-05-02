package com.colormine.banking.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.colormine.banking.R;

public class OnboardingSlideFragment extends Fragment {

    private static final String ARG_EMOJI = "emoji";
    private static final String ARG_TITLE = "title";
    private static final String ARG_DESC = "desc";

    public static OnboardingSlideFragment newInstance(String emoji, String title, String desc) {
        OnboardingSlideFragment fragment = new OnboardingSlideFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMOJI, emoji);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, desc);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_slide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        TextView tvEmoji = view.findViewById(R.id.slide_emoji);
        TextView tvTitle = view.findViewById(R.id.slide_title);
        TextView tvDesc = view.findViewById(R.id.slide_description);
        
        if (getArguments() != null) {
            tvEmoji.setText(getArguments().getString(ARG_EMOJI));
            tvTitle.setText(getArguments().getString(ARG_TITLE));
            tvDesc.setText(getArguments().getString(ARG_DESC));
        }
    }
}
