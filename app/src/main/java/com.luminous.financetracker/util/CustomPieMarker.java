package com.luminous.financetracker.util;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.luminous.financetracker.R;

public class CustomPieMarker extends MarkerView {

    private final TextView tvMarker;
    private float totalSpending;

    public CustomPieMarker(Context context, int layoutResource) {
        super(context, layoutResource);
        tvMarker = findViewById(R.id.tv_marker_text);
    }

    // We pass the total spending so we can calculate the percentage on the fly
    public void setTotalSpending(float totalSpending) {
        this.totalSpending = totalSpending;
    }

    // This method fires every time you tap a slice
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if (e instanceof PieEntry) {
            PieEntry pieEntry = (PieEntry) e;

            String category = pieEntry.getLabel();
            float amount = pieEntry.getValue();

            // Calculate percentage
            int percentage = 0;
            if (totalSpending > 0) {
                percentage = Math.round((amount / totalSpending) * 100);
            }

            // Format the text inside the white box
            String text = String.format("%s\nRM %.2f\n%d%%", category, amount, percentage);
            tvMarker.setText(text);
        }
        super.refreshContent(e, highlight);
    }

    // Centers the white box exactly over where your finger tapped
    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}