package com.bluestrom.gao.besselloadingtest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.bluestrom.gao.customview.TextureViewRingPercent;
import com.bluestrom.gao.customview.ViewClipRingPercent;

public class TextureViewTestActivity extends AppCompatActivity implements View.OnClickListener {

    private ViewClipRingPercent loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_view_test);
        loading = (ViewClipRingPercent) findViewById(R.id.loading);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.jump:
//                startActivity(new Intent(this, Main2Activity.class));
                loading.startRotate();
                break;
            default:
                break;
        }
    }
}
