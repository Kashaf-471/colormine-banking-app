package com.colormine.banking.fragments;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.colormine.banking.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private TextView tvTotalSpending, tvIncome, tvExpense;
    private LineChart lineChart;
    private DatabaseReference mDatabase;
    private String userEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        
        mDatabase = FirebaseDatabase.getInstance().getReference();
        SharedPreferences pref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userEmail = pref.getString("email", "");

        tvTotalSpending = view.findViewById(R.id.tv_total_spending);
        tvIncome = view.findViewById(R.id.tv_income);
        tvExpense = view.findViewById(R.id.tv_expense);
        lineChart = view.findViewById(R.id.line_chart);

        loadStatsFromFirebase();
        
        return view;
    }

    private void loadStatsFromFirebase() {
        if (userEmail.isEmpty()) return;

        mDatabase.child("transactions").orderByChild("user_email").equalTo(userEmail)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                double totalIncome = 0;
                    double totalExpense = 0;
                    List<Entry> chartEntries = new ArrayList<>();
                    int index = 0;

                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        Double amount = postSnapshot.child("amount").getValue(Double.class);
                        String type = postSnapshot.child("type").getValue(String.class);

                        if (amount != null) {
                            if ("INCOME".equals(type)) {
                                totalIncome += amount;
                            } else {
                                totalExpense += amount;
                            }
                            // Simplified: use index as X-axis (representing sequence of transactions)
                            chartEntries.add(new Entry(index++, amount.floatValue()));
                        }
                    }

                    animateValue(0, (float) (totalIncome - totalExpense), tvTotalSpending, true);
                    animateValue(0, (float) totalIncome, tvIncome, true);
                    animateValue(0, (float) totalExpense, tvExpense, true);
                    
                    setupChart(chartEntries);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    private void animateValue(float start, float end, TextView textView, boolean isCurrency) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            if (isCurrency) {
                NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
                String result = format.format(value);
                if (textView == tvIncome) result = "+" + result;
                if (textView == tvExpense) result = "-" + result;
                textView.setText(result);
            } else {
                textView.setText(String.valueOf((int)value));
            }
        });
        animator.start();
    }

    private void setupChart(List<Entry> entries) {
        if (lineChart == null || entries.isEmpty()) return;
        
        LineDataSet dataSet = new LineDataSet(entries, "Transactions");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        dataSet.setLineWidth(4f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary));
        dataSet.setCircleRadius(6f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(ContextCompat.getColor(requireContext(), R.color.colorSurface));
        dataSet.setCircleHoleRadius(3f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
        dataSet.setFillAlpha(40);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary));
        
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(ContextCompat.getColor(requireContext(), R.color.colorGray200));
        lineChart.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary));

        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.animateY(1000);
        lineChart.invalidate();
    }
}
