package com.colormine.banking.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.CardDetailActivity;
import com.colormine.banking.LoginSignupActivity;
import com.colormine.banking.MainActivity;
import com.colormine.banking.R;
import com.colormine.banking.SendMoneyActivity;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.models.Transaction;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private TextView tvBalance;
    private TextView tvUserName;
    private DatabaseReference mDatabase;
    private String userEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        if (userEmail.isEmpty()) {
            startActivity(new Intent(getActivity(), LoginSignupActivity.class));
            requireActivity().finish();
            return view;
        }

        tvBalance = view.findViewById(R.id.card_balance_amount);
        tvUserName = view.findViewById(R.id.tv_user_name);

        initHeader(view);
        initCard(view);
        initQuickActions(view);
        initTransactions(view);

        loadUserProfile();
        loadTransactions();

        return view;
    }

    private void loadUserProfile() {
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    String formattedBalance = String.format("$%,.2f", user.getBalance());
                    if (tvUserName != null) tvUserName.setText(user.getName());
                    if (tvBalance != null) tvBalance.setText(formattedBalance);
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

    private void initHeader(View view) {
        view.findViewById(R.id.btn_open_drawer).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });

        view.findViewById(R.id.btn_notifications).setOnClickListener(v -> {
            // Future: Open notifications activity/fragment
        });
    }

    private void initCard(View view) {
        view.findViewById(R.id.home_card).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CardDetailActivity.class));
        });
    }

    private void initQuickActions(View view) {
        view.findViewById(R.id.action_send).setOnClickListener(v ->
            startActivity(new Intent(getActivity(), SendMoneyActivity.class)));

        view.findViewById(R.id.action_cards).setOnClickListener(v ->
            startActivity(new Intent(getActivity(), CardDetailActivity.class)));

        view.findViewById(R.id.action_stats).setOnClickListener(v -> {
            if (getActivity() instanceof OnTabSwitchListener) {
                ((OnTabSwitchListener) getActivity()).onTabSwitchRequested(1); // Stats tab
            }
        });
    }

    private void initTransactions(View view) {
        rvTransactions = view.findViewById(R.id.rv_transactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionList = new ArrayList<>();

        view.findViewById(R.id.btn_view_all).setOnClickListener(v -> {
            if (getActivity() instanceof OnTabSwitchListener) {
                ((OnTabSwitchListener) getActivity()).onTabSwitchRequested(2); // History tab
            }
        });
    }

    public interface OnTabSwitchListener {
        void onTabSwitchRequested(int position);
    }
}
