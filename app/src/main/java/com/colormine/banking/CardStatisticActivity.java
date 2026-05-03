package com.colormine.banking;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CardStatisticActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView tvTotalSpending, tvIncome, tvExpense;
    private DatabaseReference mDatabase;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_statistic);

        SharedPreferences pref = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");
        mDatabase = FirebaseDatabase.getInstance().getReference();

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        lineChart = findViewById(R.id.line_chart);
        tvTotalSpending = findViewById(R.id.tv_total_spending);
        tvIncome = findViewById(R.id.tv_income);
        tvExpense = findViewById(R.id.tv_expense);

        setupChart();
        loadStatistics();
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
    }

    private void loadStatistics() {
        if (userEmail.isEmpty()) return;

        mDatabase.child("transactions").orderByChild("user_email").equalTo(userEmail)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    double totalIncome = 0;
                    double totalExpense = 0;

                    List<Entry> entries = new ArrayList<>();
                    int index = 0;

                    for (DataSnapshot snap : snapshot.getChildren()) {
                        Double amount = snap.child("amount").getValue(Double.class);
                        String type = snap.child("type").getValue(String.class);

                        if (amount != null && type != null) {
                            if (type.equals("INCOME")) {
                                totalIncome += amount;
                            } else {
                                totalExpense += amount;
                                entries.add(new Entry(index++, amount.floatValue()));
                            }
                        }
                    }

                    tvTotalSpending.setText(String.format("$%.2f", totalExpense));
                    tvIncome.setText(String.format("+$%.2f", totalIncome));
                    tvExpense.setText(String.format("-$%.2f", totalExpense));

                    updateChart(entries);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void updateChart(List<Entry> entries) {
        if (entries.isEmpty()) {
            entries.add(new Entry(0, 0f));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Spending");
        dataSet.setColor(Color.parseColor("#8B5CF6")); // colorPrimary
        dataSet.setCircleColor(Color.parseColor("#8B5CF6"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Simple X axis labels
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        lineChart.getXAxis().setLabelCount(Math.min(entries.size(), days.length));

        lineChart.invalidate(); // refresh
    }
}
