package com.filantrop.pvnclient.views;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.filantrop.pvnclient.R;

public class HomeActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
    }
}
