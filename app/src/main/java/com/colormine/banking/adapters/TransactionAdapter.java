package com.colormine.banking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.R;
import com.colormine.banking.models.Transaction;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactionList;

    public TransactionAdapter(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction tx = transactionList.get(position);
        holder.tvName.setText(tx.getName());
        holder.tvDate.setText(tx.getDate());
        holder.tvCategory.setText(tx.getCategory());

        if (tx.isIncome()) {
            holder.tvAmount.setText(String.format("+$%.2f", tx.getAmount()));
            holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorSuccess));
            holder.iconBg.setBackgroundResource(R.drawable.bg_icon_green);
            holder.ivIcon.setImageResource(R.drawable.ic_arrow_down_left);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorSuccess));
        } else {
            holder.tvAmount.setText(String.format("-$%.2f", tx.getAmount()));
            holder.tvAmount.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorError));
            holder.iconBg.setBackgroundResource(R.drawable.bg_icon_red);
            holder.ivIcon.setImageResource(R.drawable.ic_arrow_up_right);
            holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorError));
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public void updateData(List<Transaction> newList) {
        this.transactionList = newList;
        notifyDataSetChanged();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        LinearLayout iconBg;
        ImageView ivIcon;
        TextView tvName, tvDate, tvCategory, tvAmount;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBg = itemView.findViewById(R.id.transaction_icon_bg);
            ivIcon = itemView.findViewById(R.id.transaction_icon);
            tvName = itemView.findViewById(R.id.tv_transaction_name);
            tvDate = itemView.findViewById(R.id.tv_transaction_date);
            tvCategory = itemView.findViewById(R.id.tv_transaction_category);
            tvAmount = itemView.findViewById(R.id.transaction_amount);
        }
    }
}
