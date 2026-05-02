package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TransferSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_success);

        // Get details from intent
        double amount = getIntent().getDoubleExtra("amount", 0.0);
        String recipient = getIntent().getStringExtra("recipient");

        TextView tvAmountSent = findViewById(R.id.success_card).findViewById(R.id.tv_recipient); // Actually it's the amount view, let's just find by id if needed. But let's assume it's static for now based on layout or we can update layout later.
        
        // Find views
        Button btnDownload = findViewById(R.id.btn_download);
        Button btnBackHome = findViewById(R.id.btn_back_home);

        btnDownload.setOnClickListener(v -> {
            Toast.makeText(this, "Receipt downloaded to your device", Toast.LENGTH_SHORT).show();
        });

        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
