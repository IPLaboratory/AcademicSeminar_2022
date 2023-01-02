package com.ipl.gesturecontroller.util;

import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

public class Base64FileEncoder extends Thread {
    private String data;
    private final File file;

    public Base64FileEncoder(File file) {
        this.file = file;
    }

    @Override
    public void run() {
        try {
            FileInputStream fis = new FileInputStream(this.file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while((count = fis.read(buffer)) > 0) {
                baos.write(buffer, 0, count);
            }
            String result = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            result = result.replace("\\", "");
            this.data = result;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getBase64String() {
        return this.data;
    }
}
