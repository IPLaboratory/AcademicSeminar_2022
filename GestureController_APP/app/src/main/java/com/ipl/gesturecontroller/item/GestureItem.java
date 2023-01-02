package com.ipl.gesturecontroller.item;

public class GestureItem {
    private final String action;
    private final String name;

    public GestureItem(String action, String name) {
        this.action = action;
        this.name = name;
    }

    public String getAction() {
        return this.action;
    }

    public String getName() {
        return this.name;
    }
}
