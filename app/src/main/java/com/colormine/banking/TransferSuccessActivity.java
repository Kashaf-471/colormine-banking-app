package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class TransferSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_success);

        // Get details from intent
        double amount = getIntent().getDoubleExtra("amount", 0.0);
        String recipient = getIntent().getStringExtra("recipient");
        
        // Generate a random reference number for UI
        String referenceNo = "#TXN" + System.currentTimeMillis() / 1000;

        // Find views
        TextView tvAmount = findViewById(R.id.tv_amount);
        TextView tvRecipient = findViewById(R.id.tv_recipient);
        TextView tvReference = findViewById(R.id.tv_reference);
        Button btnDownload = findViewById(R.id.btn_download);
        Button btnBackHome = findViewById(R.id.btn_back_home);

        // Set data
        tvAmount.setText(String.format(Locale.getDefault(), "$%.2f", amount));
        tvRecipient.setText(recipient != null ? recipient : "Recipient");
        tvReference.setText(referenceNo);

        btnDownload.setOnClickListener(v -> {
            Toast.makeText(this, "Receipt downloaded to your device", Toast.LENGTH_SHORT).show();
        });

        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("select_tab", 0); // Go to Home tab
            startActivity(intent);
            finish();
        });
    }
}
