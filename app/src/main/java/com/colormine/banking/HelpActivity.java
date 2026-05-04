package com.colormine.banking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class HelpActivity extends BaseActivity {

    private static final int PERMISSION_REQUEST_CALL = 1;
    private static final int PERMISSION_REQUEST_LOCATION = 3;
    private static final String SUPPORT_NUMBER = "+923334968534";

    private TextView tvLocationStatus;
    private TextView tvCallLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        RelativeLayout btnCallSupport = findViewById(R.id.btn_call_support);
        RelativeLayout btnSmsSupport = findViewById(R.id.btn_sms_support);
        RelativeLayout btnGetLocation = findViewById(R.id.btn_get_location);
        RelativeLayout btnTerms = findViewById(R.id.btn_terms);
        tvLocationStatus = findViewById(R.id.tv_location_status);
        tvCallLabel = findViewById(R.id.tv_call_support_label);

        // Update UI with the actual number
        if (tvCallLabel != null) {
            tvCallLabel.setText("Call Support (" + SUPPORT_NUMBER + ")");
        }

        btnCallSupport.setOnClickListener(v -> makePhoneCall());
        btnSmsSupport.setOnClickListener(v -> sendSms());
        btnGetLocation.setOnClickListener(v -> getLocation());
        btnTerms.setOnClickListener(v -> startActivity(new Intent(this, TermsActivity.class)));
    }

    private void makePhoneCall() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_REQUEST_CALL);
        } else {
            try {
                // ACTION_CALL attempts to place the call directly
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + SUPPORT_NUMBER));
                startActivity(callIntent);
            } catch (SecurityException e) {
                openDialer();
            }
        }
    }

    private void openDialer() {
        // Fallback: ACTION_DIAL does not require permission
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + SUPPORT_NUMBER));
        startActivity(intent);
    }

    private void sendSms() {
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + SUPPORT_NUMBER));
        smsIntent.putExtra("sms_body", "Hello ColorMine Support, I need assistance with my account.");
        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open SMS app", Toast.LENGTH_SHORT).show();
        }
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        } else {
            tvLocationStatus.setText("Branch found: Gulberg III, Lahore");
            
            // Open Google Maps pointing to the location
            Uri gmmIntentUri = Uri.parse("geo:31.5204,74.3587?q=Bank");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=Bank+Lahore"));
                startActivity(webIntent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == PERMISSION_REQUEST_CALL) makePhoneCall();
            if (requestCode == PERMISSION_REQUEST_LOCATION) getLocation();
        } else {
            if (requestCode == PERMISSION_REQUEST_CALL) {
                Toast.makeText(this, "Permission denied. Opening dialer...", Toast.LENGTH_SHORT).show();
                openDialer();
            } else {
                Toast.makeText(this, "Permission required for this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
