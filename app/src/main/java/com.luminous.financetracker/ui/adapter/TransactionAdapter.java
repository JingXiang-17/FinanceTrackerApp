package com.luminous.financetracker.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.entity.Transaction;

// 1. Extend ListAdapter instead of RecyclerView.Adapter
public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionHolder> {

    public TransactionAdapter() {
        super(DIFF_CALLBACK);
    }

    // 2. The DiffUtil Callback calculates the differences between the old and new lists
    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK = new DiffUtil.ItemCallback<Transaction>() {
        @Override
        public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            // Check if the items represent the same database entry (usually by ID)
            // Note: Make sure your Transaction entity has a getId() method for its primary key!
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
            // Check if any visual details changed (amount, text, timestamp)
            return oldItem.getAmount() == newItem.getAmount() &&
                    oldItem.getTimestamp() == newItem.getTimestamp() &&
                    oldItem.getText().equals(newItem.getText());
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
        // ListAdapter provides a built-in getItem(position) method
        Transaction currentTransaction = getItem(position);

        // Populate the UI components
        holder.textViewAmount.setText(String.format("RM %.2f", currentTransaction.getAmount()));
        holder.textViewDesc.setText(currentTransaction.getText());
    }

    // 3. Inner class holding the views
    class TransactionHolder extends RecyclerView.ViewHolder {
        private TextView textViewAmount;
        private TextView textViewDesc;

        public TransactionHolder(@NonNull View itemView) {
            super(itemView);
            // Link views to the IDs you created in item_transaction.xml
            textViewAmount = itemView.findViewById(R.id.text_transaction_amount);
            textViewDesc = itemView.findViewById(R.id.text_transaction_desc);
        }
    }
}