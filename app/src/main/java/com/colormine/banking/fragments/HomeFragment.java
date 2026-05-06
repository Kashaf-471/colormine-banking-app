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

import com.colormine.banking.BaseActivity;
import com.colormine.banking.CardDetailActivity;
import com.colormine.banking.LoginSignupActivity;
import com.colormine.banking.MainActivity;
import com.colormine.banking.ManageCardsActivity;
import com.colormine.banking.NotificationsActivity;
import com.colormine.banking.R;
import com.colormine.banking.RequestMoneyActivity;
import com.colormine.banking.SendMoneyActivity;
import com.colormine.banking.adapters.TransactionAdapter;
import com.colormine.banking.models.Card;
import com.colormine.banking.models.Transaction;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.colormine.banking.utils.SettingsManager;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private TextView tvBalance, tvUserName, tvCardNumber, tvCardExpiry;
    private DatabaseReference mDatabase;
    private String userEmail;
    private ValueEventListener profileListener, transactionListener;

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
        tvCardNumber = view.findViewById(R.id.card_number);
        tvCardExpiry = view.findViewById(R.id.card_expiry);

        initHeader(view);
        initCard(view);
        initQuickActions(view);
        initTransactions(view);

        loadUserProfile();
        loadTransactions();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        applyBalanceMask();
        loadUserProfile();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (profileListener != null) {
            mDatabase.child("users").child(userEmail.replace(".", ",")).removeEventListener(profileListener);
        }
        if (transactionQuery != null && transactionListener != null) {
            transactionQuery.removeEventListener(transactionListener);
        }
    }

    private void applyBalanceMask() {
        if (!isAdded() || tvBalance == null) return;
        SettingsManager settingsManager = SettingsManager.getInstance(requireContext());
        if (settingsManager.isHideBalance()) {
            tvBalance.setText("$ ****.**");
        }
    }

    private void loadUserProfile() {
        String sanitizedEmail = userEmail.replace(".", ",");
        profileListener = mDatabase.child("users").child(sanitizedEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    SettingsManager settingsManager = SettingsManager.getInstance(requireContext());
                    boolean hideBalance = settingsManager.isHideBalance();
                    
                    if (tvUserName != null) tvUserName.setText(user.getName());
                    String primaryBalance = String.format("$%,.2f", user.getBalance());
                    Card primaryCard = null;
                    if (user.getCards() != null && !user.getCards().isEmpty()) {
                        String primaryId = user.getPrimaryCardId();
                        if (primaryId != null) {
                            for (Card c : user.getCards()) {
                                if (primaryId.equals(c.getId())) {
                                    primaryCard = c;
                                    break;
                                }
                            }
                        }
                        if (primaryCard == null) {
                            primaryCard = user.getCards().get(0);
                        }

                        primaryBalance = String.format("$%,.2f", primaryCard.getBalance());
                        if (tvCardNumber != null) tvCardNumber.setText(primaryCard.getCardNumber());
                        if (tvCardExpiry != null) tvCardExpiry.setText(primaryCard.getExpiryDate());
                    }
                    
                    if (tvBalance != null) {
                        tvBalance.setText(hideBalance ? "$ ****.**" : primaryBalance);
                    }
                    
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).updateDrawerInfo(user.getName(), user.getEmail(), hideBalance ? "$ ****.**" : primaryBalance);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private com.google.firebase.database.Query transactionQuery;

    private void loadTransactions() {
        transactionQuery = mDatabase.child("transactions").orderByChild("user_email").equalTo(userEmail);
        transactionListener = transactionQuery.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;
                    transactionList.clear();
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        String id = postSnapshot.getKey();
                        String title = postSnapshot.child("title").getValue(String.class);
                        String date = postSnapshot.child("date").getValue(String.class);
                        String category = postSnapshot.child("category").getValue(String.class);
                        Double amount = postSnapshot.child("amount").getValue(Double.class);
                        String type = postSnapshot.child("type").getValue(String.class);

                        if (amount != null) {
                            String cardId = postSnapshot.child("card_id").getValue(String.class);
                            transactionList.add(0, new Transaction(id, title, date, category, amount, "INCOME".equals(type), cardId));
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
        
        View btnNotif = view.findViewById(R.id.btn_notifications);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> {
                if (getActivity() instanceof BaseActivity) {
                    ((BaseActivity) getActivity()).playClickFeedback();
                }
                startActivity(new Intent(requireContext(), NotificationsActivity.class));
            });
        }
    }

    private void initCard(View view) {
        view.findViewById(R.id.home_card).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ManageCardsActivity.class));
        });
    }

    private void initQuickActions(View view) {
        view.findViewById(R.id.action_send).setOnClickListener(v -> {
            playFeedback();
            startActivity(new Intent(requireContext(), SendMoneyActivity.class));
        });

        view.findViewById(R.id.action_request).setOnClickListener(v -> {
            playFeedback();
            startActivity(new Intent(requireContext(), RequestMoneyActivity.class));
        });

        view.findViewById(R.id.action_cards).setOnClickListener(v -> {
            playFeedback();
            startActivity(new Intent(requireContext(), ManageCardsActivity.class));
        });

        view.findViewById(R.id.action_loan).setOnClickListener(v -> {
            playFeedback();
            startActivity(new Intent(requireContext(), com.colormine.banking.LoanRequestActivity.class));
        });
    }

    private void playFeedback() {
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).playClickFeedback();
        }
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
