package com.luminous.financetracker.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.luminous.financetracker.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private List<Map.Entry<String, Double>> categoryList = new ArrayList<>();

    public void setCategories(Map<String, Double> categories) {
        this.categoryList = new ArrayList<>(categories.entrySet());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, Double> entry = categoryList.get(position);

        String categoryName = entry.getKey();
        Double amount = entry.getValue();

        holder.tvName.setText(categoryName);
        holder.tvAmount.setText(String.format("RM %.2f", amount));

        // --- EMPTY STATE & COLOR LOGIC ---
        if (amount == 0.0) {
            // Empty Phase: Turn the card Light Grey
            holder.cardContainer.setCardBackgroundColor(Color.parseColor("#E0E0E0"));

            // Optional: Dim the icon to match the greyed-out state
            holder.ivIcon.setColorFilter(Color.parseColor("#A0A0A0"));
        } else {
            // Active Phase: Assign the correct vibrant color based on the category name
            int categoryColor = getCategoryColor(categoryName);
            holder.cardContainer.setCardBackgroundColor(categoryColor);

            // Clear any color filters on the icon so it looks normal
            holder.ivIcon.clearColorFilter();
        }
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    // Maps your categories to the exact same colors used in your Pie Chart
    private int getCategoryColor(String categoryName) {
        switch (categoryName) {
            case "Dining":
                return Color.parseColor("#D34B56"); // Red
            case "Transport":
                return Color.parseColor("#FFB12B"); // Orange-Yellow
            case "Entertainment":
                return Color.parseColor("#5BB1EB"); // Blue
            case "Shopping":
                return Color.parseColor("#EB73D3"); // Pink
            case "Others":
                return Color.parseColor("#53CF95"); // Green
            default:
                return Color.parseColor("#B4B4B4"); // Fallback Grey
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAmount;
        ImageView ivIcon;
        MaterialCardView cardContainer; // Reference to the background card

        ViewHolder(View itemView) {
            super(itemView);
            // Mapped to your existing XML IDs
            tvName = itemView.findViewById(R.id.tv_category_name);
            tvAmount = itemView.findViewById(R.id.tv_category_amount);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);

            // Assuming the root element of item_category.xml is a MaterialCardView.
            // If it has a specific ID, change this to: itemView.findViewById(R.id.YOUR_CARD_ID);
            cardContainer = (MaterialCardView) itemView;
        }
    }
}