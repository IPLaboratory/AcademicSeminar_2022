package com.ipl.gesturecontroller.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.Gesture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.ipl.gesturecontroller.R;
import com.ipl.gesturecontroller.adapter.DeviceThumbnailGridAdapter;
import com.ipl.gesturecontroller.adapter.GestureListAdapter;
import com.ipl.gesturecontroller.item.DeviceItem;
import com.ipl.gesturecontroller.item.GestureItem;
import com.ipl.gesturecontroller.util.SocketApplication;
import com.ipl.gesturecontroller.util.SpacingDecoration;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class AddGestureActivity extends AppCompatActivity {
    private RecyclerView gestureListView;
    private GestureListAdapter gestureListAdapter;
    private Socket mSocket;
    ArrayList<String> registeredGestureList;
    private Map<Integer, Map<Integer, String>> gestureMap;

    int deviceId;
    int deviceThumbnail;
    Spinner registeredGestureSpinner;
    EditText addGestureNameEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_gesture);

        // socket event add
        SocketApplication app = (SocketApplication) getApplication();
        mSocket = app.getSocket();
        mSocket.on("mapping result", mappingResult);

        gestureMap = app.getGestureMap();
        registeredGestureList = app.getRegisteredGestureList();

        deviceId = (int)getIntent().getExtras().get("Device ID");
        deviceThumbnail = (int)getIntent().getExtras().get("Thumbnail");
        String deviceName = (String)getIntent().getExtras().get("Name");

        ImageView thumbnail = findViewById(R.id.add_gesture_device_thumbnail);
        thumbnail.setImageResource(deviceThumbnail);

        TextView name = findViewById(R.id.add_gesture_device_name);
        name.setText(deviceName);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        gestureListAdapter = new GestureListAdapter(view -> {});

        if (gestureMap.containsKey(deviceId)) {
            for (int gestureId : gestureMap.get(deviceId).keySet()) {
                gestureListAdapter.addGesture(new GestureItem(gestureMap.get(deviceId).get(gestureId), registeredGestureList.get(gestureId)));
            }
        }

        SpacingDecoration gestureDecoration = new SpacingDecoration();
        gestureDecoration.setVerticalSpacing(8);

        gestureListView = findViewById(R.id.gesture_list);
        gestureListView.setHasFixedSize(true);
        gestureListView.setLayoutManager(layoutManager);
        gestureListView.setAdapter(gestureListAdapter);
        gestureListView.addItemDecoration(gestureDecoration);

        addGestureNameEdit = findViewById(R.id.add_gesture_name_edit);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, registeredGestureList);

        registeredGestureSpinner = findViewById(R.id.registered_gesture_spinner);
        registeredGestureSpinner.setAdapter(spinnerAdapter);

        Button addGestureButton = findViewById(R.id.add_gesture_button);
        addGestureButton.setOnClickListener(view -> {
            if (addGestureNameEdit.getText().toString().equals("") || registeredGestureSpinner.getSelectedItemPosition() == 0) {
                Snackbar.make(this, view, "제스처 이름과 동작을 모두 선택해주세요", Snackbar.LENGTH_SHORT).show();
                return;
            }
            if (gestureMap.get(deviceId).containsKey(registeredGestureSpinner.getSelectedItemPosition())) {
                Snackbar.make(this, view, "이미 추가된 제스처입니다", Snackbar.LENGTH_SHORT).show();
                return;
            }

            try {
                Log.i("SEND SOCKET", "send did & gid to server");
                JSONObject json = new JSONObject();
                json.put("did", deviceId);
                json.put("gid", registeredGestureSpinner.getSelectedItemPosition());
                mSocket.emit("gesture mapping", json);
                Toast.makeText(this, "무선 컨트롤러에 리모컨 신호를 입력하세요.", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            gestureMap.get(deviceId).put(registeredGestureSpinner.getSelectedItemPosition(), addGestureNameEdit.getText().toString());
//            String action = registered_gestures.get(registeredGestureSpinner.getSelectedItemPosition());
//            gestureListAdapter.addGesture(new GestureItem(addGestureNameEdit.getText().toString(), action));
//            gestureListAdapter.notifyDataSetChanged();
//            gestureListView.setAdapter(gestureListAdapter);
        });

        FloatingActionButton registerGestureFab = findViewById(R.id.register_gesture_fab);
        registerGestureFab.setOnClickListener(view -> {
            Intent intent = new Intent(this, RegisterGestureActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.off("mapping result", mappingResult);
    }

    private Emitter.Listener mappingResult = args -> {
        JSONObject data = (JSONObject) args[0];
        try {
            int did = data.getInt("did");
            int gid = data.getInt("gid");
            int mappingResult = data.getInt("result");
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                if (mappingResult == 1) {   // 제스처 저장 성공
                    Toast.makeText(getApplicationContext(), "제스처 저장 성공", Toast.LENGTH_SHORT).show();
                    // 디바이스 아이디, 제스처 아이디 이름 이케 저장하는 곳인듯
                    gestureMap.get(deviceId).put(registeredGestureSpinner.getSelectedItemPosition(), addGestureNameEdit.getText().toString());
                    String action = registeredGestureList.get(registeredGestureSpinner.getSelectedItemPosition());
                    gestureListAdapter.addGesture(new GestureItem(addGestureNameEdit.getText().toString(), action));
                    gestureListAdapter.notifyDataSetChanged();
                    gestureListView.setAdapter(gestureListAdapter);
                } else if (mappingResult == 2) {    // 제스처 저장 실패(중복)
                    Toast.makeText(getApplicationContext(), "제스처 저장 실패", Toast.LENGTH_SHORT).show();
                } else if (mappingResult == 0) {    // 제스처 저장 실패(디비에러)
                    Toast.makeText(getApplicationContext(), "제스처 저장 실패", Toast.LENGTH_SHORT).show();
                }
            }, 0);

            Log.i("MAPPING RESULT", "did: " + did + " gid: " + gid + " mapping_result: " + mappingResult);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    };


}