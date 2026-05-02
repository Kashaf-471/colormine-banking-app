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

        // Find views
        Button btnDownload = findViewById(R.id.btn_download);
        Button btnBackHome = findViewById(R.id.btn_back_home);

        btnDownload.setOnClickListener(v -> {
            Toast.makeText(this, "Receipt downloaded to your device", Toast.LENGTH_SHORT).show();
        });

        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(MainActivity.EXTRA_SELECT_TAB, 0); // Go to Home tab
            startActivity(intent);
            finish();
        });
    }
}
