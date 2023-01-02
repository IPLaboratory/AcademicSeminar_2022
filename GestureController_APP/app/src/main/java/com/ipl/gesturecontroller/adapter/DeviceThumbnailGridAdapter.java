package com.ipl.gesturecontroller.adapter;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ipl.gesturecontroller.R;
import com.ipl.gesturecontroller.item.DeviceThumbnailGridItem;

import java.util.ArrayList;

public class DeviceThumbnailGridAdapter extends ArrayAdapter {
    private Context context;
    private int layoutResourceId;
    private ArrayList<DeviceThumbnailGridItem> data;

    public DeviceThumbnailGridAdapter(@NonNull Context context, int layoutResourceId, ArrayList data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        DeviceThumbViewHolder holder = null;

        if (view == null) { // 재활용된 View를 사용되어 null check -> 새로 인스턴스화 하여 view 생성
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(layoutResourceId, parent, false);
            holder = new DeviceThumbViewHolder();
            holder.name = (TextView) view.findViewById(R.id.deviceThumbnailText);
            holder.image = (ImageView) view.findViewById(R.id.deviceThumbnail);
            view.setTag(holder);
        } else {
            holder = (DeviceThumbViewHolder) view.getTag();
        }

        DeviceThumbnailGridItem item = data.get(position);
        holder.name.setText(item.getDeviceName());
        holder.image.setImageBitmap(item.getImage());

        return view;
    }

    static class DeviceThumbViewHolder {
        ImageView image;
        TextView name;
    }
}
