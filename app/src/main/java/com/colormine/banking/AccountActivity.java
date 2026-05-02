package com.colormine.banking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatActivity;

public class AccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        RelativeLayout optionPersonalInfo = findViewById(R.id.option_personal_info);
        RelativeLayout optionMyCards = findViewById(R.id.option_my_cards);
        RelativeLayout optionNotifications = findViewById(R.id.option_notifications);
        RelativeLayout optionSecurity = findViewById(R.id.option_security);
        RelativeLayout optionSettings = findViewById(R.id.option_settings);
        RelativeLayout optionHelp = findViewById(R.id.option_help);
        Button btnLogout = findViewById(R.id.btn_logout);

        optionMyCards.setOnClickListener(v -> startActivity(new Intent(this, CardDetailActivity.class)));
        optionSecurity.setOnClickListener(v -> startActivity(new Intent(this, SecurityActivity.class)));
        optionSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        optionHelp.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));

        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginSignupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
