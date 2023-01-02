package com.ipl.gesturecontroller.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ipl.gesturecontroller.util.Base64FileEncoder;
import com.ipl.gesturecontroller.R;
import com.ipl.gesturecontroller.util.SocketApplication;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Audio;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Mode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class RegisterGestureActivity extends AppCompatActivity {
    private Socket mSocket;
    private Button startButton;
    private EditText registerGestureNameEdit;
    private ArrayList<String> registeredGestureList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_gesture);

        SocketApplication app = (SocketApplication) getApplication();
        mSocket = app.getSocket();
        mSocket.on("send mlResult", registerResult);

        registeredGestureList = app.getRegisteredGestureList();

        // Check for camera access permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
        }

        CameraView camera = findViewById(R.id.camera_preview);
        camera.setLifecycleOwner(this);
        camera.setMode(Mode.VIDEO);
        camera.setFacing(Facing.FRONT);
        camera.setUseDeviceOrientation(false);
        camera.setAudio(Audio.OFF);
        camera.setPlaySounds(false);
        camera.addCameraListener(new VideoTakenListener());

        registerGestureNameEdit = findViewById(R.id.register_gesture_name_edit);

        startButton = findViewById(R.id.start_register_gesture_button);
        startButton.setOnClickListener(view -> {
            if (registerGestureNameEdit.getText().toString().equals("")) {
                Toast.makeText(this, "제스처 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            if (registeredGestureList.contains(registerGestureNameEdit.getText().toString())) {
                Toast.makeText(this, "이미 등록된 제스처입니다", Toast.LENGTH_SHORT).show();
            }
            startRecord(camera);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.off("send mlResult", registerResult);
    }

    class VideoTakenListener extends CameraListener {
        @Override
        public void onVideoRecordingStart() {
            super.onVideoRecordingStart();
            startButton.setEnabled(false);
            new Thread(() -> {
                for (int i = 10; i > 0; --i) {
                    int finalI = i;
                    runOnUiThread(() -> startButton.setText("남은 시간: " + finalI));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            super.onVideoTaken(result);

            Toast.makeText(getApplicationContext(), "데이터 전송 중", Toast.LENGTH_SHORT).show();

            Base64FileEncoder encoderThread = new Base64FileEncoder(result.getFile());
            encoderThread.start();
            try {
                encoderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String encodedData = encoderThread.getBase64String();

            Thread sendData = new Thread(() -> {
                int chunk_size = 500000; //700000
                for (int i = 0; i < encodedData.length(); i += chunk_size) {
                    int min = Math.min(encodedData.length(), i + chunk_size);
                    JSONObject message =  new JSONObject();
                    try {
                        message.put("all", encodedData.length());
                        message.put("count", min);
                        message.put("gesture_id", registeredGestureList.size());
                        message.put("frame", encodedData.substring(i, min));
                        mSocket.emit("add gesture", message);

                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.d("Send Image", "" + ((double)min / (double)encodedData.length()) * 100.0 + "% Image was uploaded to Server!");
                }

                if (result.getFile().delete()) {
                    Log.d("Delete", "file deleted");
                }
            });

            sendData.start();
            try {
                sendData.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            registeredGestureList.add(registerGestureNameEdit.getText().toString());

            startButton.setText("데이터 학습 중...");
        }
    }

    private void startRecord(CameraView camera) {
        File saveFile = new File(getFilesDir(), "gesture.mp4");
        camera.takeVideo(saveFile, 10000);
    }

    private Emitter.Listener registerResult = args -> {
        JSONObject data = (JSONObject) args[0];
        try {
            boolean result = data.getBoolean("result");
            if (result) {
                finish();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    };
}