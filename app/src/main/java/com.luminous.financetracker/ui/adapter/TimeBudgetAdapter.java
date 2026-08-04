package com.luminous.financetracker.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.luminous.financetracker.R;

public class TimeBudgetAdapter extends RecyclerView.Adapter<TimeBudgetAdapter.BudgetHolder> {

    private final String[] titles = {"Daily budget", "Weekly budget", "Monthly budget"};
    private double[] spentAmounts = {0.0, 0.0, 0.0};
    private double[] budgetLimits = {30.0, 200.0, 1000.0};

    // --- NEW: Edit Click Listener ---
    private OnBudgetEditListener listener;

    public interface OnBudgetEditListener {
        void onEditClick(int position, String title, double currentLimit);
    }

    public void setOnBudgetEditListener(OnBudgetEditListener listener) {
        this.listener = listener;
    }
    // --------------------------------

    public void updateSpentAmount(int position, double spent) {
        this.spentAmounts[position] = spent;
        notifyItemChanged(position);
    }

    public void updateLimit(int position, double limit) {
        this.budgetLimits[position] = limit;
        notifyItemChanged(position);
    }

    @NonNull
    @Override
    public BudgetHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_budget, parent, false);
        return new BudgetHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetHolder holder, int position) {
        holder.tvTitle.setText(titles[position]);
        holder.tvSpent.setText(String.format("RM %.2f", spentAmounts[position]));
        holder.tvLimit.setText(String.format("/ RM %.2f", budgetLimits[position]));

        double limit = budgetLimits[position];
        double spent = spentAmounts[position];
        int percent = (limit > 0) ? (int) ((spent / limit) * 100) : 0;

        holder.tvPercent.setText(percent + "%");

        holder.progressBar.setMax(100);
        holder.progressBar.setProgress(Math.min(percent, 100));
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    class BudgetHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle, tvSpent, tvLimit, tvPercent;
        private ProgressBar progressBar;
        private ImageView btnEdit;

        public BudgetHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_budget_time_title);
            tvSpent = itemView.findViewById(R.id.tv_budget_spent);
            tvLimit = itemView.findViewById(R.id.tv_budget_limit);
            tvPercent = itemView.findViewById(R.id.tv_budget_percent);
            progressBar = itemView.findViewById(R.id.progress_time_budget);
            btnEdit = itemView.findViewById(R.id.btn_edit_budget);

            // --- NEW: Trigger the click listener ---
            btnEdit.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (listener != null && pos != RecyclerView.NO_POSITION) {
                    listener.onEditClick(pos, titles[pos], budgetLimits[pos]);
                }
            });
        }
    }
}