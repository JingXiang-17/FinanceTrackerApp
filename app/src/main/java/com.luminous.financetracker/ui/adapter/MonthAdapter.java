package com.luminous.financetracker.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.luminous.financetracker.R;
import java.util.Calendar;

public class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.ViewHolder> {

    private final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private final int currentYear = Calendar.getInstance().get(Calendar.YEAR);

    // Default to the current real-world month (0 = Jan, 11 = Dec)
    private int selectedPosition = Calendar.getInstance().get(Calendar.MONTH);
    private OnMonthClickListener listener;

    public interface OnMonthClickListener {
        void onMonthClick(int monthIndex);
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
        holder.tvYear.setText(String.valueOf(currentYear)); // Set the year dynamically

        // Highlight the selected month using the MaterialCardView
        if (selectedPosition == position) {
            holder.cardBg.setCardBackgroundColor(Color.parseColor("#9A56B9")); // Purple background
            holder.tvMonth.setTextColor(Color.parseColor("#FFFFFF")); // White text
        } else {
            holder.cardBg.setCardBackgroundColor(Color.parseColor("#E0E0E0")); // Light Gray background
            holder.tvMonth.setTextColor(Color.parseColor("#888888")); // Dark Gray text
        }

        holder.itemView.setOnClickListener(v -> {
            int previousItem = selectedPosition;
            selectedPosition = position;

            // Re-render just the two items that changed state
            notifyItemChanged(previousItem);
            notifyItemChanged(selectedPosition);

            // Tell the Activity to update the chart
            if (listener != null) {
                listener.onMonthClick(selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return months.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth, tvYear;
        MaterialCardView cardBg;

        ViewHolder(View itemView) {
            super(itemView);
            // Mapped to your new XML IDs
            tvMonth = itemView.findViewById(R.id.tv_month_name);
            tvYear = itemView.findViewById(R.id.tv_month_year);
            cardBg = itemView.findViewById(R.id.card_month_bg);
        }
    }
}