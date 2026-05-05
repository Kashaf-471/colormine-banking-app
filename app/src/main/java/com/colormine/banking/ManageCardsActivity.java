package com.colormine.banking;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.CardAdapter;
import com.colormine.banking.models.Card;
import com.colormine.banking.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ManageCardsActivity extends AppCompatActivity {

    private RecyclerView rvCards;
    private CardAdapter adapter;
    private List<Card> cardList;
    private DatabaseReference mDatabase;
    private String userEmail;
    private String primaryCardId;
    private ValueEventListener cardsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_cards);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        if (userEmail.isEmpty()) {
            finish();
            return;
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add_card).setOnClickListener(v -> {
            startActivity(new Intent(this, AddCardActivity.class));
        });

        rvCards = findViewById(R.id.rv_cards);
        rvCards.setLayoutManager(new LinearLayoutManager(this));
        cardList = new ArrayList<>();
        
        adapter = new CardAdapter(cardList, primaryCardId, new CardAdapter.OnCardClickListener() {
            @Override
            public void onCardClick(Card card) {
                Intent intent = new Intent(ManageCardsActivity.this, CardDetailActivity.class);
                intent.putExtra("CARD_ID", card.getId());
                startActivity(intent);
            }

            @Override
            public void onMakePrimary(Card card) {
                setPrimaryCard(card.getId());
            }
        });
        rvCards.setAdapter(adapter);

        loadCards();
    }

    private void loadCards() {
        String sanitizedEmail = userEmail.replace(".", ",");
        cardsListener = mDatabase.child("users").child(sanitizedEmail).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    cardList.clear();
                    if (user.getCards() != null) {
                        cardList.addAll(user.getCards());
                    }
                    primaryCardId = user.getPrimaryCardId();
                    adapter.updateData(cardList, primaryCardId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cardsListener != null) {
            mDatabase.child("users").child(userEmail.replace(".", ",")).removeEventListener(cardsListener);
        }
    }

    private void setPrimaryCard(String cardId) {
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("users").child(sanitizedEmail).child("primaryCardId").setValue(cardId)
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Primary card updated", Toast.LENGTH_SHORT).show());
    }
}
