package com.ipl.gesturecontroller.activity;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.ipl.gesturecontroller.R;
import com.ipl.gesturecontroller.adapter.DeviceThumbnailGridAdapter;
import com.ipl.gesturecontroller.item.DeviceThumbnailGridItem;

import java.util.ArrayList;

public class AddDeviceActivity extends AppCompatActivity {
    private GridView gridView;  // 디바이스 섬네일 그리드뷰
    private DeviceThumbnailGridAdapter gridAdapter; // 그리드 어댑터
    private Button registBtn;
    private EditText nameEditText;

    private int selectedItemPos=-1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_thumbnail_grid);

        gridView = (GridView) findViewById(R.id.deviceGirdView);
        gridAdapter = new DeviceThumbnailGridAdapter(this, R.layout.device_thumbnail_grid_item, getData());
        gridView.setAdapter(gridAdapter);

        gridView.setOnItemClickListener((adapterView, view, position, id) -> {
            selectedItemPos = position;
            gridView.setSelection(position);
            gridView.setItemChecked(position, true);

            for (int i = 0; i < gridAdapter.getCount(); i++) {
                if ((i != position)) {  // 선택된 아이템이 아니면
                    MaterialCardView mcView = gridView.getChildAt(i).findViewById(R.id.thumbnailCardView);
                    mcView.setStrokeColor(Color.TRANSPARENT);
                } else {    //선택된 아이템이라면 이미지 뷰를 가져와 배경색 변경
                    MaterialCardView mcView =  view.findViewById(R.id.thumbnailCardView);
                    mcView.setStrokeColor(getColor(R.color.primary_light));
                    Log.d("SELECT DEVICE", ((TextView)view.findViewById(R.id.deviceThumbnailText)).getText().toString());
                }
            }
        });

        nameEditText = findViewById(R.id.deviceNameInputText);
        registBtn = findViewById(R.id.deviceNameInputBtn);
        registBtn.setOnClickListener(view -> {
            Editable name = nameEditText.getText();
            ArrayList<String> registeredNames = (ArrayList<String>) getIntent().getSerializableExtra("Device Names");

            if(selectedItemPos==-1 || (name.length()==0)){  // 이미지 선택안함 | 이름 입력 안함
                Toast.makeText(getApplicationContext(), "아이콘과 이름을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
            }else if(registeredNames.stream().anyMatch(n -> n.equals(name.toString()))) {   // 중복 이름
                Toast.makeText(getApplicationContext(), "중복된 이름입니다..", Toast.LENGTH_SHORT).show();
            }else { // 새로운 기기에 등록
                Log.d("SELECTED INDEX", String.valueOf(selectedItemPos));
                TypedArray images = getResources().obtainTypedArray(R.array.image_ids);
                int resourceId = images.getResourceId(selectedItemPos, -1);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("Thumbnail", resourceId);
                resultIntent.putExtra("Name", name.toString());
                setResult(RESULT_OK, resultIntent);
                finish();

            }
        });
    }

    private ArrayList<DeviceThumbnailGridItem> getData() {
        final ArrayList<DeviceThumbnailGridItem> gridItems = new ArrayList<>();
        TypedArray images = getResources().obtainTypedArray(R.array.image_ids);
        String[] names = getResources().getStringArray(R.array.image_names);
        for (int i = 0; i < images.length(); i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), images.getResourceId(i, -1));
            gridItems.add(new DeviceThumbnailGridItem(bitmap, names[i]));
        }
        return gridItems;
    }
}
