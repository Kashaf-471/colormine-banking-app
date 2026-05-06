package com.colormine.banking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.colormine.banking.models.Card;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminLoansActivity extends BaseActivity {

    private ListView listView;
    private DatabaseReference mDatabase;
    private List<Map<String, Object>> loanRequests;
    private LoanAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_loans);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        listView = findViewById(R.id.list_loan_requests);
        loanRequests = new ArrayList<>();
        adapter = new LoanAdapter();
        listView.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        loadLoanRequests();
    }

    private void loadLoanRequests() {
        mDatabase.child("loan_requests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                loanRequests.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Map<String, Object> loan = (Map<String, Object>) snap.getValue();
                    if (loan != null) {
                        loanRequests.add(0, loan); // Newest first
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showGrantDialog(Map<String, Object> loan) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_grant_loan, null);
        TextView tvInfo = view.findViewById(R.id.tv_dialog_loan_info);
        android.widget.EditText etDuration = view.findViewById(R.id.et_duration_days);
        Button btnCancel = view.findViewById(R.id.btn_cancel_grant);
        Button btnConfirm = view.findViewById(R.id.btn_confirm_grant);

        tvInfo.setText("User: " + loan.get("userName") + "\nAmount: $" + String.format("%.2f", ((Number) loan.get("amount")).doubleValue()));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(view)
            .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String durationStr = etDuration.getText().toString().trim();
            if (durationStr.isEmpty()) {
                etDuration.setError("Required");
                return;
            }
            try {
                int selectedDays = Integer.parseInt(durationStr);
                if (selectedDays <= 0) {
                    etDuration.setError("Must be positive");
                    return;
                }
                grantLoan(loan, selectedDays);
                dialog.dismiss();
            } catch (NumberFormatException e) {
                etDuration.setError("Invalid number");
            }
        });

        dialog.show();
    }

    private void grantLoan(Map<String, Object> loan, int durationDays) {
        String loanId = (String) loan.get("id");
        String userId = (String) loan.get("userId");
        String cardId = (String) loan.get("cardId");
        double amount = ((Number) loan.get("amount")).doubleValue();

        long repaymentTime = System.currentTimeMillis() + (durationDays * 24L * 60 * 60 * 1000);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "GRANTED");
        updates.put("grantedTimestamp", System.currentTimeMillis());
        updates.put("repaymentTimestamp", repaymentTime);
        updates.put("durationDays", durationDays);

        mDatabase.child("loan_requests").child(loanId).updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                transferLoanAmountToUser(userId, cardId, amount, loanId);
            })
            .addOnFailureListener(e -> Toast.makeText(AdminLoansActivity.this, "Failed to update loan status", Toast.LENGTH_SHORT).show());
    }

    private void transferLoanAmountToUser(String userId, String cardId, double amount, String loanId) {
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getCards() != null) {
                    List<Card> cards = user.getCards();
                    boolean cardFound = false;
                    for (Card card : cards) {
                        if (card.getId().equals(cardId)) {
                            card.setBalance(card.getBalance() + amount);
                            cardFound = true;
                            break;
                        }
                    }
                    
                    if (!cardFound) {
                        // Fallback to first card if original card not found
                        if (!cards.isEmpty()) {
                            cards.get(0).setBalance(cards.get(0).getBalance() + amount);
                        } else {
                            Toast.makeText(AdminLoansActivity.this, "User has no cards to receive funds", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    
                    user.syncGlobalBalance();
                    
                    Map<String, Object> userUpdates = new HashMap<>();
                    userUpdates.put("cards", cards);
                    userUpdates.put("balance", user.getBalance());

                    mDatabase.child("users").child(userId).updateChildren(userUpdates)
                        .addOnSuccessListener(aVoid -> {
                            createLoanTransaction(userId, cardId, amount);
                            notifyUser(userId, amount);
                            Toast.makeText(AdminLoansActivity.this, "Loan granted and funds transferred to user card", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(AdminLoansActivity.this, "Failed to transfer funds", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void createLoanTransaction(String userId, String cardId, double amount) {
        String txId = mDatabase.child("transactions").push().getKey();
        Map<String, Object> tx = new HashMap<>();
        tx.put("title", "Bank Loan Credit");
        tx.put("date", new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date()));
        tx.put("category", "Loan");
        tx.put("amount", amount);
        tx.put("type", "INCOME");
        tx.put("user_email", userId.replace(",", "."));
        tx.put("card_id", cardId);
        tx.put("timestamp", System.currentTimeMillis());

        if (txId != null) {
            mDatabase.child("transactions").child(txId).setValue(tx);
        }
    }

    private void notifyUser(String userId, double amount) {
        String notifId = mDatabase.child("notifications").child(userId).push().getKey();
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", "Loan Approved!");
        notif.put("message", "Your loan request of $" + String.format("%.2f", amount) + " has been approved and credited to your card.");
        notif.put("timestamp", System.currentTimeMillis());
        notif.put("type", "loan");

        if (notifId != null) {
            mDatabase.child("notifications").child(userId).child(notifId).setValue(notif);
        }
    }

    private class LoanAdapter extends BaseAdapter {
        @Override
        public int getCount() { return loanRequests.size(); }
        @Override
        public Object getItem(int position) { return loanRequests.get(position); }
        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(AdminLoansActivity.this).inflate(R.layout.item_loan_request, parent, false);
            }

            Map<String, Object> loan = loanRequests.get(position);
            TextView tvName = convertView.findViewById(R.id.tv_user_name);
            TextView tvAmount = convertView.findViewById(R.id.tv_loan_amount);
            TextView tvCard = convertView.findViewById(R.id.tv_card_details);
            TextView tvStatus = convertView.findViewById(R.id.tv_status);
            View layoutActions = convertView.findViewById(R.id.layout_actions);
            Button btnGrant = convertView.findViewById(R.id.btn_grant);
            Button btnReject = convertView.findViewById(R.id.btn_reject);

            tvName.setText((String) loan.get("userName"));
            tvAmount.setText(String.format("$%,.2f", ((Number) loan.get("amount")).doubleValue()));
            tvCard.setText((String) loan.get("cardDetails"));

            String status = (String) loan.get("status");
            if ("PENDING".equals(status)) {
                layoutActions.setVisibility(View.VISIBLE);
                tvStatus.setVisibility(View.GONE);
            } else {
                layoutActions.setVisibility(View.GONE);
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(status);
                if ("GRANTED".equals(status)) {
                    tvStatus.setTextColor(getResources().getColor(R.color.colorSuccess));
                } else if ("REPAID".equals(status)) {
                    tvStatus.setTextColor(getResources().getColor(R.color.colorPrimary));
                } else {
                    tvStatus.setTextColor(getResources().getColor(R.color.colorError));
                }
            }

            btnGrant.setOnClickListener(v -> showGrantDialog(loan));
            btnReject.setOnClickListener(v -> {
                mDatabase.child("loan_requests").child((String) loan.get("id")).child("status").setValue("REJECTED");
            });

            return convertView;
        }
    }
}
