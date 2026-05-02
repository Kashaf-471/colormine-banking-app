package com.colormine.banking;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

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
        
        List<Contact> contacts = new ArrayList<>();
        contacts.add(new Contact("1", "Sarah J.", "@sarahj", "👩"));
        contacts.add(new Contact("2", "Mike T.", "@miket", "👨"));
        contacts.add(new Contact("3", "Alex R.", "@alexr", "🧑"));
        contacts.add(new Contact("4", "Emma W.", "@emmaw", "👱‍♀️"));
        
        ContactAdapter adapter = new ContactAdapter(contacts, contact -> {
            selectedContact = contact;
            // Update UI to show selected contact
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
                if (amount <= 0) {
                    Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Intent intent = new Intent(this, TransferSuccessActivity.class);
                intent.putExtra("amount", amount);
                intent.putExtra("recipient", selectedContact.getName());
                startActivity(intent);
                finish();
                
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
