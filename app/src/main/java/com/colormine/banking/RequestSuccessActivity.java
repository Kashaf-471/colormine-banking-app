package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class RequestSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_success);

        // Get details from intent
        double amount = getIntent().getDoubleExtra("amount", 0.0);
        String recipient = getIntent().getStringExtra("recipient");

        // Find views
        TextView tvAmount = findViewById(R.id.tv_amount);
        TextView tvRecipient = findViewById(R.id.tv_recipient);
        TextView tvRef = findViewById(R.id.tv_ref);
        Button btnBackHome = findViewById(R.id.btn_back_home);

        // Set data
        tvAmount.setText(String.format("$%.2f", amount));
        if (recipient != null) {
            tvRecipient.setText(recipient);
        }
        
        // Generate random ref number for visual effect
        String randomRef = "#REQ" + (int)(Math.random() * 10000000);
        tvRef.setText(randomRef);

        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(MainActivity.EXTRA_SELECT_TAB, 0); // Go to Home tab
            startActivity(intent);
            finish();
        });
    }
}
