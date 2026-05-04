package com.colormine.banking;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.adapters.NotificationAdapter;
import com.colormine.banking.models.Notification;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private TextView tvEmpty;
    private DatabaseReference mDatabase;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        recyclerView = findViewById(R.id.rv_notifications);
        tvEmpty = findViewById(R.id.tv_empty_notifications);
        ImageButton btnBack = findViewById(R.id.btn_back);

        notificationList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        if (!userEmail.isEmpty()) {
            loadNotifications();
        }
    }

    private void loadNotifications() {
        String sanitizedEmail = userEmail.replace(".", ",");
        mDatabase.child("notifications").child(sanitizedEmail)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    notificationList.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        try {
                            Notification notification = dataSnapshot.getValue(Notification.class);
                            if (notification != null) {
                                notification.setId(dataSnapshot.getKey());
                                notificationList.add(notification);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // Show newest first
                    Collections.sort(notificationList, (n1, n2) -> 
                        Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                    
                    adapter.updateData(notificationList);
                    tvEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(NotificationsActivity.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                }
            });
    }
}
