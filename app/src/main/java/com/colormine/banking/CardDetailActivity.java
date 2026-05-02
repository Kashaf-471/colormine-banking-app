package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CardDetailActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvCvv, tvCvvAction;
    private boolean isCvvVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_detail);

        btnBack = findViewById(R.id.btn_back);
        tvCvv = findViewById(R.id.tv_cvv);
        tvCvvAction = findViewById(R.id.tv_cvv_action);

        btnBack.setOnClickListener(v -> finish());

        // Card Actions
        LinearLayout actionViewStats = findViewById(R.id.action_view_stats);
        LinearLayout actionCopyCard = findViewById(R.id.action_copy_card);
        LinearLayout actionToggleCvv = findViewById(R.id.action_toggle_cvv);
        LinearLayout actionLockCard = findViewById(R.id.action_lock_card);
        Button btnSendMoney = findViewById(R.id.btn_send_money);

        actionViewStats.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_SELECT_TAB, 1); // Stats tab
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        
        actionCopyCard.setOnClickListener(v -> {
            Toast.makeText(this, "Card number copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        actionToggleCvv.setOnClickListener(v -> {
            isCvvVisible = !isCvvVisible;
            if (isCvvVisible) {
                tvCvv.setText("123");
                tvCvvAction.setText(R.string.action_hide_cvv);
            } else {
                tvCvv.setText(R.string.cvv_masked);
                tvCvvAction.setText(R.string.action_show_cvv);
            }
        });

        actionLockCard.setOnClickListener(v -> {
            Toast.makeText(this, "Card temporarily locked", Toast.LENGTH_SHORT).show();
        });

        btnSendMoney.setOnClickListener(v -> startActivity(new Intent(this, SendMoneyActivity.class)));
    }
}
