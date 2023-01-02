package com.ipl.gesturecontroller.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ipl.gesturecontroller.R;
import com.ipl.gesturecontroller.item.GestureItem;

import java.util.ArrayList;


public class GestureListAdapter extends RecyclerView.Adapter<GestureListAdapter.GestureViewHolder> {
    private final ArrayList<GestureItem> items;
    private final View.OnClickListener onClickListener;

    public static class GestureViewHolder extends RecyclerView.ViewHolder {
        private final TextView action;
        private final TextView name;

        public GestureViewHolder(View itemView) {
            super(itemView);
            this.action = itemView.findViewById(R.id.gesture_action);
            this.name = itemView.findViewById(R.id.gesture_name);
        }
    }

    public GestureListAdapter(View.OnClickListener onClickListener) {
        this.items = new ArrayList<>();
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public GestureViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gesture_item, parent, false);
        view.setOnClickListener(this.onClickListener);
        return new GestureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(GestureViewHolder holder, int position) {
        holder.action.setText(items.get(position).getAction());
        holder.name.setText(items.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addGesture(GestureItem item) {
        this.items.add(item);
        this.notifyItemInserted(this.items.size() - 1);
    }

    public GestureItem getGesture(int position) {
        return this.items.get(position);
    }
}
