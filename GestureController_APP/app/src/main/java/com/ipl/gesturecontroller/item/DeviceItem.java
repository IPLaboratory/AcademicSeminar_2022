package com.ipl.gesturecontroller.item;

public class DeviceItem {
    private final int thumbnail;
    private final String name;

    public DeviceItem(int thumbnail, String name) {
        this.thumbnail = thumbnail;
        this.name = name;
    }

    public int getThumbnail() {
        return this.thumbnail;
    }

    public String getName() {
        return this.name;
    }
}
