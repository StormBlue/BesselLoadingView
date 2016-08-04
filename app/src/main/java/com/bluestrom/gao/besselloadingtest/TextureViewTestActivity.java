package com.bluestrom.gao.besselloadingtest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.bluestrom.gao.customview.TextureViewBesselLoading;

public class TextureViewTestActivity extends AppCompatActivity implements View.OnClickListener {

    private TextureViewBesselLoading loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_texture_view_test);
        loading = (TextureViewBesselLoading) findViewById(R.id.loading);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.jump:
                startActivity(new Intent(this, Main2Activity.class));
                break;
            default:
                break;
        }
    }
}
