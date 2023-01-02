package com.ipl.gesturecontroller.util;

import android.app.Application;
import android.util.Log;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketApplication extends Application {
    private Map<Integer, Map<Integer, String>> gestureMap = new HashMap();
    {
        gestureMap.put(0, new HashMap<Integer, String>(){{
            put(1, "전원 On");
            put(2, "전원 Off");
        }});
        gestureMap.put(1, new HashMap<Integer, String>(){{
            put(1, "전원 On/Off");
            put(2, "조명 On/Off");
        }});
        gestureMap.put(2, new HashMap<Integer, String>(){{
            put(1, "전원 On/Off");
        }});
        gestureMap.put(3, new HashMap<Integer, String>(){{
            put(3, "볼륨 Down");
            put(4, "볼륨 Up");
        }});
    }

    private ArrayList<String> registeredGestureList = new ArrayList<>();
    {
        registeredGestureList.add("제스처를 선택하세요.");
        registeredGestureList.add("Up");
        registeredGestureList.add("Down");
        registeredGestureList.add("One Finger Range Decrease");
        registeredGestureList.add("One Finger Range Increase");
        registeredGestureList.add("Two Finger Range Decrease");
        registeredGestureList.add("Two Finger Range Increase");
        registeredGestureList.add("Dumpling");
    }

    private Socket mSocket;
    {
        try {
            // mSocket = IO.socket("http://192.168.0.7:8080");
            mSocket = IO.socket("http://220.69.208.237:8080");
            Log.i("[START]", "socket instance created");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    public Map<Integer, Map<Integer, String>> getGestureMap() {
        return gestureMap;
    }

    public ArrayList<String> getRegisteredGestureList() {
        return registeredGestureList;
    }

}
