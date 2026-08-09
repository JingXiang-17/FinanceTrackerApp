package com.luminous.financetracker.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.luminous.financetracker.R;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.util.TimeUtils;

import java.util.Objects;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionHolder> {

    private OnItemClickListener listener;

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
            // Include all relevant fields to ensure smooth UI updates when edited
            return oldItem.getAmount() == newItem.getAmount() &&
                    oldItem.isExpanded() == newItem.isExpanded() &&
                    Objects.equals(oldItem.getText(), newItem.getText()) &&
                    Objects.equals(oldItem.getCategory(), newItem.getCategory()) &&
                    Objects.equals(oldItem.getPaymentMethod(), newItem.getPaymentMethod()) &&
                    Objects.equals(oldItem.getMerchantName(), newItem.getMerchantName()) &&
                    Objects.equals(oldItem.getNotes(), newItem.getNotes());
        }
    };

    @NonNull
    @Override
    public TransactionHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionHolder holder, int position) {
        Transaction currentTransaction = getItem(position);

        holder.tvTitle.setText(currentTransaction.getText());
        holder.tvAmount.setText(String.format("-RM %.2f", currentTransaction.getAmount()));

        // Clarify labels for the expanded view based on our prior discussion
        holder.tvCategory.setText("Category: " + currentTransaction.getCategory());
        holder.tvPayment.setText("From: " + currentTransaction.getPaymentMethod());
        holder.tvMerchant.setText("To: " + currentTransaction.getMerchantName());
        holder.tvTime.setText("Time: " + TimeUtils.formatTimestamp(currentTransaction.getTimestamp()));

        if (currentTransaction.getNotes() != null && !currentTransaction.getNotes().trim().isEmpty()) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText("Notes: " + currentTransaction.getNotes());
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        // Toggle Expand/Collapse visibility
        holder.layoutExpandedDetails.setVisibility(currentTransaction.isExpanded() ? View.VISIBLE : View.GONE);

        // Click header to expand
        holder.layoutHeader.setOnClickListener(v -> {
            currentTransaction.setExpanded(!currentTransaction.isExpanded());
            notifyItemChanged(position); // Triggers re-render for this specific item
        });
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public interface OnItemClickListener {
        void onEditClick(Transaction transaction);
        void onDeleteClick(Transaction transaction);
    }

    class TransactionHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle, tvAmount;
        private TextView tvCategory, tvMerchant, tvPayment, tvTime, tvNotes;
        private ImageView btnEdit, btnDelete;
        private View layoutHeader;
        private LinearLayout layoutExpandedDetails;

        public TransactionHolder(View itemView) {
            super(itemView);
            layoutHeader = itemView.findViewById(R.id.layout_header);
            layoutExpandedDetails = itemView.findViewById(R.id.layout_expanded_details);

            tvTitle = itemView.findViewById(R.id.tv_transaction_title);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvCategory = itemView.findViewById(R.id.tv_detail_category);
            tvMerchant = itemView.findViewById(R.id.tv_detail_merchant);
            tvPayment = itemView.findViewById(R.id.tv_detail_payment);
            tvTime = itemView.findViewById(R.id.tv_detail_time);
            tvNotes = itemView.findViewById(R.id.tv_detail_notes);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);

            btnEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onEditClick(getItem(position));
                }
            });

            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();

                // Ensure the item still exists before doing anything
                if (listener != null && position != RecyclerView.NO_POSITION) {

                    // Launch the confirmation popup
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(v.getContext())
                            .setTitle("Delete Transaction")
                            .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
                            .setPositiveButton("Delete transaction", (dialog, which) -> {
                                // They clicked Yes, so execute your original delete logic
                                listener.onDeleteClick(getItem(position));
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                // They clicked Cancel, just close the popup
                                dialog.dismiss();
                            })
                            .show();
                }
            });
        }
    }
}