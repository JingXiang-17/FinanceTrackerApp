package com.luminous.financetracker.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.luminous.financetracker.R;
import java.util.Calendar;

public class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.ViewHolder> {

    private final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    // The adapter dictates its own time context
    private final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    private int selectedPosition = Calendar.getInstance().get(Calendar.MONTH);
    private OnMonthClickListener listener;

    // FIX: Upgraded signature to strictly enforce both year and month context
    public interface OnMonthClickListener {
        void onMonthClick(int year, int monthIndex);
    }

    public MonthAdapter(OnMonthClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvMonth.setText(months[position]);
        holder.tvYear.setText(String.valueOf(currentYear));

        if (position == selectedPosition) {
            holder.cardBg.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.cardBgActive));
            holder.tvMonth.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.textMonthActive));
            holder.tvYear.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.textYearActive));
        } else {
            holder.cardBg.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.cardBgInactive));
            holder.tvMonth.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.textMonthInactive));
            holder.tvYear.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.textMonthInactive));
        }

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            int previousItem = selectedPosition;
            selectedPosition = currentPosition;

            notifyItemChanged(previousItem);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                // FIX: Pass the absolute year alongside the index to prevent Activity guesswork
                listener.onMonthClick(currentYear, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return months.length;
    }

    public void setSelectedIndex(int newIndex) {
        int previousIndex = selectedPosition;
        selectedPosition = newIndex;

        if (previousIndex != -1) {
            notifyItemChanged(previousIndex);
        }

        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth, tvYear;
        MaterialCardView cardBg;

        ViewHolder(View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tv_month_name);
            tvYear = itemView.findViewById(R.id.tv_month_year);
            cardBg = itemView.findViewById(R.id.card_month_bg);
        }
    }
}