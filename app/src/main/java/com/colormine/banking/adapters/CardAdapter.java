package com.colormine.banking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.colormine.banking.R;
import com.colormine.banking.models.Card;
import java.util.List;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private List<Card> cardList;
    private String primaryCardId;
    private OnCardClickListener listener;

    public interface OnCardClickListener {
        void onCardClick(Card card);
        void onMakePrimary(Card card);
    }

    public CardAdapter(List<Card> cardList, String primaryCardId, OnCardClickListener listener) {
        this.cardList = cardList;
        this.primaryCardId = primaryCardId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cardList.get(position);
        holder.tvCardNumber.setText(card.getCardNumber());
        holder.tvCardHolder.setText(card.getCardHolderName());
        holder.tvExpiryDate.setText(card.getExpiryDate());
        holder.tvCvv.setText(card.getCvv());
        holder.tvBalance.setText(String.format("$%,.2f", card.getBalance()));
        
        boolean isPrimary = card.getId().equals(primaryCardId);
        holder.tvPrimaryBadge.setVisibility(isPrimary ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCardClick(card);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null && !isPrimary) {
                listener.onMakePrimary(card);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    public void updateData(List<Card> newList, String primaryId) {
        this.cardList = newList;
        this.primaryCardId = primaryId;
        notifyDataSetChanged();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        TextView tvCardNumber, tvCardHolder, tvExpiryDate, tvCvv, tvPrimaryBadge, tvBalance;
        ImageView ivCardType;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardNumber = itemView.findViewById(R.id.tv_card_number);
            tvCardHolder = itemView.findViewById(R.id.tv_card_holder);
            tvExpiryDate = itemView.findViewById(R.id.tv_expiry_date);
            tvCvv = itemView.findViewById(R.id.tv_cvv);
            tvPrimaryBadge = itemView.findViewById(R.id.tv_primary_badge);
            tvBalance = itemView.findViewById(R.id.tv_card_balance);
            ivCardType = itemView.findViewById(R.id.iv_card_type);
        }
    }
}
