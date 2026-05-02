package com.colormine.banking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class HelpActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CALL = 1;
    private static final int PERMISSION_REQUEST_SMS = 2;
    private static final int PERMISSION_REQUEST_LOCATION = 3;

    private TextView tvLocationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        RelativeLayout btnCallSupport = findViewById(R.id.btn_call_support);
        RelativeLayout btnSmsSupport = findViewById(R.id.btn_sms_support);
        RelativeLayout btnGetLocation = findViewById(R.id.btn_get_location);
        tvLocationStatus = findViewById(R.id.tv_location_status);

        btnCallSupport.setOnClickListener(v -> makePhoneCall());
        btnSmsSupport.setOnClickListener(v -> sendSms());
        btnGetLocation.setOnClickListener(v -> getLocation());
    }

    private void makePhoneCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_REQUEST_CALL);
        } else {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:18001234567"));
            startActivity(callIntent);
        }
    }

    private void sendSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_SMS);
        } else {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage("18001234567", null, "I need support with my ColorMine account.", null, null);
                Toast.makeText(this, "Support SMS sent", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        } else {
            // For course requirement, just mock the location fetch
            tvLocationStatus.setText("Branch found: 2 miles away");
            Toast.makeText(this, "Location acquired", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_REQUEST_CALL: makePhoneCall(); break;
                case PERMISSION_REQUEST_SMS: sendSms(); break;
                case PERMISSION_REQUEST_LOCATION: getLocation(); break;
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
