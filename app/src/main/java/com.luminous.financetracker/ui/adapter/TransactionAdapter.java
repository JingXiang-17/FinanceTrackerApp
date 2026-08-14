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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.luminous.financetracker.R;
import com.luminous.financetracker.data.entity.Transaction;
import com.luminous.financetracker.util.TimeUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import android.content.Context;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.TransactionHolder> {

    private OnItemClickListener listener;

    // FIX: Track expansion state independently by transaction ID to prevent UI collapse on LiveData re-emits
    private final Set<Integer> expandedIds = new HashSet<>();

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
            // FIX: Added timestamp to ensure DiffUtil re-renders if the date/time changes
            return oldItem.getAmount() == newItem.getAmount() &&
                    oldItem.getTimestamp() == newItem.getTimestamp() &&
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

        // Check independent expansion tracking state
        boolean isExpanded = expandedIds.contains(currentTransaction.getId());
        holder.layoutExpandedDetails.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Toggle Expand/Collapse visibility state securely
        holder.layoutHeader.setOnClickListener(v -> {
            if (expandedIds.contains(currentTransaction.getId())) {
                expandedIds.remove(currentTransaction.getId());
            } else {
                expandedIds.add(currentTransaction.getId());
            }
            notifyItemChanged(position);
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
                if (position != RecyclerView.NO_POSITION) {
                    // Call the unified method. Passing null for Runnable since button clicks don't need a bounce-back
                    confirmDeletion(position, v.getContext(), null);
                }
            });
        }
    }

    // Unified deletion method for both button clicks and swipes
    public void confirmDeletion(int position, Context context, Runnable onCancel) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
                .setPositiveButton("Delete transaction", (d, which) -> {
                    Transaction target = getItem(position);
                    expandedIds.remove(target.getId());
                    if (listener != null) {
                        listener.onDeleteClick(target);
                    }
                })
                .setNegativeButton("Cancel", (d, which) -> {
                    d.dismiss();
                    if (onCancel != null) onCancel.run();
                })
                .setOnCancelListener(d -> {
                    if (onCancel != null) onCancel.run();
                })
                .create();

        // You must call show() before you can access and modify the buttons
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
    }
}