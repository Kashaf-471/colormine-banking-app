package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CardDetailActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvCardNumber, tvCardHolder, tvExpiry, tvCvv, tvCvvAction;
    private boolean isCvvVisible = false;
    private DatabaseReference mDatabase;
    private String userEmail;
    private String realCvv = "000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_detail);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        btnBack = findViewById(R.id.btn_back);
        tvCardNumber = findViewById(R.id.tv_card_number);
        tvCardHolder = findViewById(R.id.tv_card_holder);
        tvExpiry = findViewById(R.id.tv_expiry_date);
        tvCvv = findViewById(R.id.tv_cvv);
        tvCvvAction = findViewById(R.id.tv_cvv_action);

        btnBack.setOnClickListener(v -> finish());

        loadCardDetails();

        // Card Actions
        LinearLayout actionViewStats = findViewById(R.id.action_view_stats);
        LinearLayout actionCopyCard = findViewById(R.id.action_copy_card);
        LinearLayout actionToggleCvv = findViewById(R.id.action_toggle_cvv);
        LinearLayout actionLockCard = findViewById(R.id.action_lock_card);
        Button btnSendMoney = findViewById(R.id.btn_send_money);

        actionViewStats.setOnClickListener(v -> {
            startActivity(new Intent(this, CardStatisticActivity.class));
        });
        
        actionCopyCard.setOnClickListener(v -> {
            if (tvCardNumber != null) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Card Number", tvCardNumber.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Card number copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        actionToggleCvv.setOnClickListener(v -> {
            isCvvVisible = !isCvvVisible;
            if (isCvvVisible) {
                tvCvv.setText(realCvv);
                tvCvvAction.setText(R.string.action_hide_cvv);
            } else {
                tvCvv.setText(R.string.cvv_masked);
                tvCvvAction.setText(R.string.action_show_cvv);
            }
        });

        actionLockCard.setOnClickListener(v -> {
            Toast.makeText(this, "Card status updated in secure vault", Toast.LENGTH_SHORT).show();
        });

        btnSendMoney.setOnClickListener(v -> startActivity(new Intent(this, SendMoneyActivity.class)));
    }

    private void loadCardDetails() {
        if (userEmail.isEmpty()) return;
        
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (tvCardNumber != null) tvCardNumber.setText(user.getCardNumber());
                    if (tvCardHolder != null) tvCardHolder.setText(user.getName());
                    if (tvExpiry != null) tvExpiry.setText(user.getCardExpiry());
                    realCvv = user.getCardCvv();
                    
                    // Reset CVV view
                    isCvvVisible = false;
                    if (tvCvv != null) tvCvv.setText(R.string.cvv_masked);
                    if (tvCvvAction != null) tvCvvAction.setText(R.string.action_show_cvv);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
