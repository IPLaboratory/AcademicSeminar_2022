package com.ipl.gesturecontroller.item;

import android.graphics.Bitmap;

public class DeviceThumbnailGridItem {
    private Bitmap image;
    private String deviceName;

    public DeviceThumbnailGridItem(Bitmap image, String deviceName){
        super();
        this.image = image;
        this.deviceName = deviceName;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
