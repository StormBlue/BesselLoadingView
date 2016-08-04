package com.bluestrom.gao.besselloadingtest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.bluestrom.gao.customview.KrundBesselLoading;
import com.bluestrom.gao.customview.SurfaceViewBesselLoading;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SurfaceViewBesselLoading besselLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        besselLoading = (SurfaceViewBesselLoading)findViewById(R.id.loading);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_loading:
//                loading.startRotate();
                break;
            case R.id.end_loading:
//                loading.stopRotate();
                break;
            default:
                break;
        }
    }
}
