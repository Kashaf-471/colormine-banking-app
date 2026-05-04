package com.colormine.banking;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.Locale;

public class TransferSuccessActivity extends BaseActivity {

    private String referenceNo;
    private double amount;
    private String recipient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_success);

        // Get details from intent
        amount = getIntent().getDoubleExtra("amount", 0.0);
        recipient = getIntent().getStringExtra("recipient");
        if (recipient == null) recipient = "Recipient";
        
        // Generate a random reference number
        referenceNo = "#TXN" + System.currentTimeMillis() / 1000;

        // Find views
        TextView tvAmount = findViewById(R.id.tv_amount);
        TextView tvRecipient = findViewById(R.id.tv_recipient);
        TextView tvReference = findViewById(R.id.tv_reference);
        Button btnDownload = findViewById(R.id.btn_download);
        Button btnShare = findViewById(R.id.btn_share);
        Button btnBackHome = findViewById(R.id.btn_back_home);

        // Set data
        tvAmount.setText(String.format(Locale.getDefault(), "$%.2f", amount));
        tvRecipient.setText(recipient);
        tvReference.setText(referenceNo);

        btnDownload.setOnClickListener(v -> {
            playClickFeedback();
            downloadReceipt();
        });

        btnShare.setOnClickListener(v -> {
            playClickFeedback();
            shareViaWhatsApp();
        });

        btnBackHome.setOnClickListener(v -> {
            playClickFeedback();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("select_tab", 0);
            startActivity(intent);
            finish();
        });

        playSuccessFeedback();
    }

    private void shareViaWhatsApp() {
        String message = "💳 *Transfer Receipt - ColorMine Bank*\n\n" +
                "💰 *Amount:* " + String.format(Locale.getDefault(), "$%.2f", amount) + "\n" +
                "👤 *Recipient:* " + recipient + "\n" +
                "🆔 *Reference:* " + referenceNo + "\n\n" +
                "✅ Transaction Successful!";

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);
        intent.setPackage("com.whatsapp");

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            // WhatsApp not installed, use generic share
            intent.setPackage(null);
            startActivity(Intent.createChooser(intent, "Share Receipt"));
        }
    }

    private void downloadReceipt() {
        View receiptCard = findViewById(R.id.success_card);
        if (receiptCard == null) return;

        Bitmap bitmap = Bitmap.createBitmap(receiptCard.getWidth(), receiptCard.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        receiptCard.draw(canvas);

        String fileName = "Receipt_" + System.currentTimeMillis() + ".png";
        
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ColorMine");
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        
        try {
            if (uri != null) {
                OutputStream out = getContentResolver().openOutputStream(uri);
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();
                    Toast.makeText(this, "Receipt saved to Gallery", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving receipt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
