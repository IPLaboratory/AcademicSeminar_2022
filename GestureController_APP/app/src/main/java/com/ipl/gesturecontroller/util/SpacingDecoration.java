package com.ipl.gesturecontroller.util;

import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SpacingDecoration extends RecyclerView.ItemDecoration {
    private int horizontalSpacing;
    private int verticalSpacing;

    public SpacingDecoration() {
        this.horizontalSpacing = 0;
        this.verticalSpacing = 0;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        try {
            GridLayoutManager.LayoutParams params = (GridLayoutManager.LayoutParams)view.getLayoutParams();
            int maxColumn = params.getSpanSize();
            int column = params.getSpanIndex();
            int position = parent.getChildAdapterPosition(view);

            int leftPixel = column * this.horizontalSpacing / maxColumn;
            int rightPixel = this.horizontalSpacing - (column + 1) * this.horizontalSpacing / maxColumn;

            outRect.left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, leftPixel, view.getResources().getDisplayMetrics());
            outRect.right = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rightPixel, view.getResources().getDisplayMetrics());
            if (position > maxColumn) {
                outRect.top = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.verticalSpacing, view.getResources().getDisplayMetrics());
            }
        } catch (ClassCastException e) {
            outRect.top = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.verticalSpacing, view.getResources().getDisplayMetrics());
        }

    }

    public void setHorizontalSpacing(int spacing) {
        this.horizontalSpacing = spacing;
    }

    public void setVerticalSpacing(int spacing) {
        this.verticalSpacing = spacing;
    }
}
