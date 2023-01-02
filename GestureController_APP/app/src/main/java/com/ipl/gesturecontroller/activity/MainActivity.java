package com.ipl.gesturecontroller.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ipl.gesturecontroller.item.DeviceItem;
import com.ipl.gesturecontroller.adapter.DeviceListAdapter;
import com.ipl.gesturecontroller.R;
import com.ipl.gesturecontroller.util.SocketApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {
    private RecyclerView deviceListView;
    private DeviceListAdapter deviceListAdapter;
    private Socket mSocket;
    boolean isConnected = false;
    private Map<Integer, Map<Integer, String>> gestureMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //socket event add
        SocketApplication app = (SocketApplication) getApplication();
        mSocket = app.getSocket();
        mSocket.on(Socket.EVENT_CONNECT,onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT,onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on("my message", onNewMessage);
        mSocket.on("request name", payload);
        mSocket.connect();

        gestureMap = app.getGestureMap();

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);

        deviceListAdapter = new DeviceListAdapter(view -> {
            int position = deviceListView.getChildAdapterPosition(view);
            DeviceItem device = ((DeviceListAdapter) Objects.requireNonNull(deviceListView.getAdapter())).getDevice(position);

            Intent intent = new Intent(this, AddGestureActivity.class);
            intent.putExtra("Device ID", position);
            intent.putExtra("Thumbnail", device.getThumbnail());
            intent.putExtra("Name", device.getName());
            startActivity(intent);
        });
        deviceListAdapter.addDevice(new DeviceItem(R.drawable.image_6, "전등"));
        deviceListAdapter.addDevice(new DeviceItem(R.drawable.image_5, "선풍기(소)"));
        deviceListAdapter.addDevice(new DeviceItem(R.drawable.image_5, "선풍기(대)"));
        deviceListAdapter.addDevice(new DeviceItem(R.drawable.image_9, "TV"));

        deviceListView = findViewById(R.id.device_list);
        deviceListView.setHasFixedSize(true);
        deviceListView.setLayoutManager(layoutManager);
        deviceListView.setAdapter(deviceListAdapter);

        ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent resultData = result.getData();
                if (resultData == null) return;
                int thumbnail = (int)resultData.getExtras().get("Thumbnail");
                String name = (String)resultData.getExtras().get("Name");
                deviceListAdapter.addDevice(new DeviceItem(thumbnail, name));
                gestureMap.put(deviceListAdapter.getItemCount() - 1, new HashMap<>());
            }
        });

        FloatingActionButton addDeviceFab = findViewById(R.id.add_device_fab);
        addDeviceFab.setOnClickListener(view -> {
            ArrayList<String> deviceNames = new ArrayList<>();
            for (int i = 0; i < deviceListAdapter.getItemCount(); ++i) {
                deviceNames.add(deviceListAdapter.getDevice(i).getName());
            }

            Intent intent = new Intent(this, AddDeviceActivity.class);
            intent.putExtra("Device Names", deviceNames);
            startForResult.launch(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT,onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT,onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off("request name", payload);
        mSocket.off("my message", onNewMessage);
    }

    private Emitter.Listener onConnect = args -> {
        if(!isConnected){
            isConnected = true;
            Log.i("Connected", "server-client connected");
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> Toast.makeText(MainActivity.this, "connected", Toast.LENGTH_SHORT).show(), 0);
        }

    };

    private Emitter.Listener onDisconnect = args -> {

        Log.i("Disconnected", "server-client disconnected");
        if (isConnected){
            isConnected = false;
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> Toast.makeText(MainActivity.this, "disconnected", Toast.LENGTH_SHORT).show(), 0);            }


    };

    private Emitter.Listener onConnectError = args -> {
        if(isConnected){
            Log.e("Disconnected", "socket connection error");
            isConnected = false;
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> Toast.makeText(MainActivity.this, "connecting error", Toast.LENGTH_SHORT).show(), 0);
        }

    };

    private Emitter.Listener payload = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            try{
                String msg = data.getString("msg");
                Log.i("REQUEST_NAME", "server: "+msg);

                JSONObject sendJson = new JSONObject("{\"device\":\"phone\"}");
                mSocket.emit("client registration", sendJson);
                Log.i("SEND", sendJson.toString());
            }catch (JSONException e){
                Log.e("ERROR", "ERROR");
            }
        }
    };
    private Emitter.Listener onNewMessage = args -> {
        JSONObject data = (JSONObject) args[0];
        String msg = data.toString();
        Log.i("MESSAGE", "server: "+msg);
    };
}