package com.filantrop.pvnclient.views;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;
import com.filantrop.pvnclient.views.callback.DBFutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;


public class SplashActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    private FptnServerViewModel fptnViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        initializeVariable();
    }

    private void initializeVariable() {
        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        ListenableFuture<List<FptnServerDto>> allServersListFuture = fptnViewModel.getAllServers();
        Futures.addCallback(allServersListFuture, (DBFutureCallback<List<FptnServerDto>>) result -> {
            new Handler().postDelayed(() -> {
                // Проверка условия (например, если пользователь залогинен)
                boolean isLoggedIn = (result != null && !result.isEmpty()); // miss login
                if (isLoggedIn) {
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish();
            }, 1500); // delay to show logo
        }, this.getMainExecutor());
    }
}
