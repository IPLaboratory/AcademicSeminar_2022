package com.ipl.gesturecontroller.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ipl.gesturecontroller.item.DeviceItem;
import com.ipl.gesturecontroller.R;

import java.util.ArrayList;


public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {
    private final ArrayList<DeviceItem> items;
    private final View.OnClickListener onClickListener;

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final TextView name;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            this.thumbnail = itemView.findViewById(R.id.device_thumbnail);
            this.name = itemView.findViewById(R.id.device_name);
        }
    }

    public DeviceListAdapter(View.OnClickListener onClickListener) {
        this.items = new ArrayList<>();
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        view.setOnClickListener(this.onClickListener);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        holder.thumbnail.setImageResource(items.get(position).getThumbnail());
        holder.name.setText(items.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addDevice(DeviceItem item) {
        this.items.add(item);
        this.notifyItemInserted(this.items.size() - 1);
    }

    public DeviceItem getDevice(int position) {
        return this.items.get(position);
    }
}
