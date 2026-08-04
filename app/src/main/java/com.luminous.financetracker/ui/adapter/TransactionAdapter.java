package com.luminous.financetracker.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.entity.Transaction;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionHolder> {

    // 1. --- ADDED: The Interface for the Click Listener ---
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditClick(Transaction transaction);
        void onDeleteClick(Transaction transaction);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    // -----------------------------------------------------

    public TransactionAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<Transaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            // 2. --- UPDATED: Added category check so the UI updates when you edit a category! ---
            return oldItem.getAmount() == newItem.getAmount() &&
                    oldItem.getTimestamp() == newItem.getTimestamp() &&
                    oldItem.getText().equals(newItem.getText()) &&
                    oldItem.getCategory().equals(newItem.getCategory());
        }
    };

    @NonNull
    @Override
    public TransactionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionHolder holder, int position) {
        Transaction currentTransaction = getItem(position);

        holder.textViewAmount.setText(String.format("RM %.2f", currentTransaction.getAmount()));
        holder.textViewTitle.setText(currentTransaction.getText());
    }

    class TransactionHolder extends RecyclerView.ViewHolder {
        private TextView textViewAmount;
        private TextView textViewTitle;
        private ImageView ivEdit;
        private ImageView ivDelete;

        public TransactionHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.tv_transaction_title);
            textViewAmount = itemView.findViewById(R.id.tv_transaction_amount);
            ivEdit = itemView.findViewById(R.id.iv_edit);
            ivDelete = itemView.findViewById(R.id.iv_delete);

            // Listen for Edit clicks
            ivEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onEditClick(getItem(position));
                }
            });

            // Listen for Delete clicks
            ivDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onDeleteClick(getItem(position));
                }
            });
        }
    }
}