package org.fptn.vpn.views;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.fptn.vpn.R;
import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.repository.FptnServerRepository;
import org.fptn.vpn.views.callback.DBFutureCallback;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.Optional;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        initializeVariable();
        throw new RuntimeException("Test Crash");
    }

    private void initializeVariable() {
        FptnServerRepository fptnServerRepository = new FptnServerRepository(getApplicationContext());
        ListenableFuture<List<FptnServerDto>> allServersListFuture = fptnServerRepository.getAllServersListFuture();
        Futures.addCallback(allServersListFuture, (DBFutureCallback<List<FptnServerDto>>) serverDtoList -> {
            Log.d(TAG, "SplashActivity.initializeVariable: serverDtoList size: " + Optional.ofNullable(serverDtoList).map(List::size).orElse(0));
            // Проверка условия (например, если пользователь залогинен)
            boolean isLoggedIn = (serverDtoList != null && !serverDtoList.isEmpty()); // miss login
            if (isLoggedIn) {
                startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                finish();
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, this.getMainExecutor());
    }

}
