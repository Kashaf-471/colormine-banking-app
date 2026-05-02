package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.ContactAdapter;
import com.colormine.banking.database.DatabaseHelper;
import com.colormine.banking.models.Contact;
import java.util.ArrayList;
import java.util.List;

public class SendMoneyActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private RecyclerView rvContacts;
    private EditText etAmount;
    private Button btn100, btn500, btn1000, btnContinue;
    
    private LinearLayout selectedContactInfo;
    private TextView tvSelectedAvatar, tvSelectedName, tvSelectedUsername;
    private Contact selectedContact = null;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

        dbHelper = new DatabaseHelper(this);
        initViews();
        setupContactsRecyclerView();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        rvContacts = findViewById(R.id.rv_contacts);
        etAmount = findViewById(R.id.et_amount);
        
        btn100 = findViewById(R.id.btn_100);
        btn500 = findViewById(R.id.btn_500);
        btn1000 = findViewById(R.id.btn_1000);
        btnContinue = findViewById(R.id.btn_continue);
        
        selectedContactInfo = findViewById(R.id.selected_contact_info);
        tvSelectedAvatar = findViewById(R.id.tv_selected_avatar);
        tvSelectedName = findViewById(R.id.tv_selected_name);
        tvSelectedUsername = findViewById(R.id.tv_selected_username);
    }

    private void setupContactsRecyclerView() {
        rvContacts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        // Using real emails for testing with the database
        List<Contact> contacts = new ArrayList<>();
        contacts.add(new Contact("1", "Sarah Johnson", "sarah@example.com", "👩"));
        contacts.add(new Contact("2", "Admin User", "admin@example.com", "👨"));
        contacts.add(new Contact("3", "Test Account", "test@example.com", "🧑"));
        
        ContactAdapter adapter = new ContactAdapter(contacts, contact -> {
            selectedContact = contact;
            selectedContactInfo.setVisibility(View.VISIBLE);
            tvSelectedAvatar.setText(contact.getAvatarInitial());
            tvSelectedName.setText(contact.getName());
            tvSelectedUsername.setText(contact.getUsername());
        });
        rvContacts.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btn100.setOnClickListener(v -> etAmount.setText("100"));
        btn500.setOnClickListener(v -> etAmount.setText("500"));
        btn1000.setOnClickListener(v -> etAmount.setText("1000"));
        
        btnContinue.setOnClickListener(v -> {
            if (selectedContact == null) {
                Toast.makeText(this, "Please select a recipient", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                double amount = Double.parseDouble(amountStr);
                performTransaction(amount);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performTransaction(double amount) {
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String senderEmail = pref.getString("email", "");

        if (senderEmail.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        double senderBalance = dbHelper.getUserBalance(senderEmail);

        if (senderBalance < amount) {
            Toast.makeText(this, "Insufficient balance!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Deduct from sender
        dbHelper.updateBalance(senderEmail, senderBalance - amount);
        dbHelper.addTransaction(senderEmail, "EXPENSE", amount, "Sent to " + selectedContact.getName(), "Transfer");

        // 2. Add to recipient
        double recipientBalance = dbHelper.getUserBalance(selectedContact.getUsername()); // Username field stores email here
        dbHelper.updateBalance(selectedContact.getUsername(), recipientBalance + amount);
        dbHelper.addTransaction(selectedContact.getUsername(), "INCOME", amount, "Received from " + senderEmail, "Transfer");

        // 3. Move to success screen
        Intent intent = new Intent(this, TransferSuccessActivity.class);
        intent.putExtra("amount", amount);
        intent.putExtra("recipient", selectedContact.getName());
        startActivity(intent);
        finish();
    }
}
