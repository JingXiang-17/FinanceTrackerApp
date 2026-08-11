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
import com.luminous.financetracker.util.Constants;

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
            holder.ivIcon.setColorFilter(Color.parseColor("#A0A0A0"));
        } else {
            // Active Phase: Assign the correct vibrant color sourced from Constants
            int categoryColor = getCategoryColor(categoryName);
            holder.cardContainer.setCardBackgroundColor(categoryColor);
            holder.ivIcon.clearColorFilter();
        }
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    // FIX: Unified category color mapping using Constants to prevent multi-file drift
    private int getCategoryColor(String categoryName) {
        if (Constants.CATEGORY_FIXED.equals(categoryName)) {
            return Color.parseColor("#A498FA"); // Light Purple
        } else if (Constants.CATEGORY_DINING.equals(categoryName)) {
            return Color.parseColor("#FC5B68"); // Red
        } else if (Constants.CATEGORY_TRANSPORT.equals(categoryName)) {
            return Color.parseColor("#FFB12B"); // Orange-Yellow
        } else if (Constants.CATEGORY_ENTERTAINMENT.equals(categoryName)) {
            return Color.parseColor("#5BB1EB"); // Blue
        } else if (Constants.CATEGORY_SHOPPING.equals(categoryName)) {
            return Color.parseColor("#EB73D3"); // Pink
        } else if (Constants.CATEGORY_OTHERS.equals(categoryName)) {
            return Color.parseColor("#53CF95"); // Green
        } else {
            return Color.parseColor("#B4B4B4"); // Fallback Grey
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAmount;
        ImageView ivIcon;
        MaterialCardView cardContainer;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_category_name);
            tvAmount = itemView.findViewById(R.id.tv_category_amount);
            ivIcon = itemView.findViewById(R.id.iv_category_icon);
            cardContainer = (MaterialCardView) itemView;
        }
    }
}