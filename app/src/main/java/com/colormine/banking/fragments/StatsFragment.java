package com.colormine.banking.fragments;

import android.animation.ValueAnimator;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private TextView tvTotalSpending, tvIncome, tvExpense;
    private LineChart lineChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stats, container, false);
        
        tvTotalSpending = view.findViewById(R.id.tv_total_spending);
        tvIncome = view.findViewById(R.id.tv_income);
        tvExpense = view.findViewById(R.id.tv_expense);
        lineChart = view.findViewById(R.id.line_chart);

        // Animate spending and stats
        animateValue(0, 3450, tvTotalSpending, true);
        animateValue(0, 4200, tvIncome, true);
        animateValue(0, 1450, tvExpense, true);
        
        setupChart();
        
        return view;
    }

    private void animateValue(float start, float end, TextView textView, boolean isCurrency) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(1500);
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

    private void setupChart() {
        if (lineChart == null) return;
        
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 1500f));
        entries.add(new Entry(1, 2200f));
        entries.add(new Entry(2, 1800f));
        entries.add(new Entry(3, 3100f));
        entries.add(new Entry(4, 2800f));
        entries.add(new Entry(5, 3600f));

        LineDataSet dataSet = new LineDataSet(entries, "Spending");
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
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary));
        
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(ContextCompat.getColor(requireContext(), R.color.colorGray200));
        lineChart.getAxisLeft().setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary));

        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.animateY(1500);
        lineChart.invalidate();
    }
}
