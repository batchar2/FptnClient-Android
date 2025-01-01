package com.filantrop.pvnclient.views;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.ViewModelProvider;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.enums.SharedPreferencesFields;
import com.filantrop.pvnclient.views.adapter.FptnServerAdapter;
import com.filantrop.pvnclient.services.CustomVpnService;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;
import com.filantrop.pvnclient.views.callback.DBFutureCallback;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class HomeActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    @Getter
    private FptnServerViewModel fptnViewModel;

    private TextView downloadTextView;
    private TextView uploadTextView;

    private TextView homeTextViewTimeLabel;
    private TextView timerTextView;
    private TextView statusTextView;
    private TextView errorTextView;

    private View homeDownloadImageView;
    private View homeUploadImageView;

    private Spinner spinnerServers;

    private ToggleButton startStopButton;

    //for service binding
    private ServiceConnection connection;
    private CustomVpnService vpnService;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        initializeVariable();
    }

    @Override
    protected void onStart() {
        super.onStart();

        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(TAG, "onServiceConnected: " + name);
                CustomVpnService.LocalBinder localBinder = (CustomVpnService.LocalBinder) service;
                vpnService = localBinder.getService();
                vpnService.setFptnViewModel(fptnViewModel);
                vpnService.updateConnectionStateInViewModel();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "onServiceDisconnected: " + name);
                vpnService.setFptnViewModel(null);
            }
        };
        bindService(getServiceIntent().setAction("ON_BIND"), connection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(connection);
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        ListenableFuture<List<FptnServerDto>> allServersFuture = fptnViewModel.getAllServers();
        Futures.addCallback(allServersFuture, (DBFutureCallback<List<FptnServerDto>>) result -> {
            List<FptnServerDto> fixedServers = new ArrayList<>();
            fixedServers.add(new FptnServerDto("Auto", "Auto", "Auto", "", 0));
            fixedServers.addAll(result);
            ((FptnServerAdapter) spinnerServers.getAdapter()).setFptnServerDtoList(fixedServers);
        }, this.getMainExecutor());
        spinnerServers = findViewById(R.id.home_server_spinner);
        spinnerServers.setAdapter(new FptnServerAdapter());

        startStopButton = findViewById(R.id.toggleButton);
        startStopButton.setOnClickListener(this::onClickToStartStop);

        homeTextViewTimeLabel = findViewById(R.id.homeTextViewTimeLabel);
        downloadTextView = findViewById(R.id.downloadTextView);
        homeDownloadImageView = findViewById(R.id.homeDownloadImageView);
        uploadTextView = findViewById(R.id.uploadTextView);
        homeUploadImageView = findViewById(R.id.homeUploadImageView);
        homeDownloadImageView = findViewById(R.id.homeDownloadImageView);

        timerTextView = findViewById(R.id.homeTextViewTime);
        statusTextView = findViewById(R.id.homeTextViewConnectionStatus);
        errorTextView = findViewById(R.id.errorTextView);


        fptnViewModel.getConnectionStateMutableLiveData().observe(this, connectionState -> {
            switch (connectionState) {
                case CONNECTING:
                    connectingStateUiItems();
                    break;
                case CONNECTED:
                    connectedStateUiItems();
                    break;
                case DISCONNECTED:
                    disconnectedStateUiItems();
            }
        });
        fptnViewModel.getDownloadSpeedAsStringLiveData().observe(this, downloadSpeed -> downloadTextView.setText(downloadSpeed));
        fptnViewModel.getUploadSpeedAsStringLiveData().observe(this, uploadSpeed -> uploadTextView.setText(uploadSpeed));
        fptnViewModel.getTimerTextLiveData().observe(this, text -> timerTextView.setText(text));
        fptnViewModel.getErrorTextLiveData().observe(this, errorText -> {
            Log.i(TAG, "errorText: " + errorText);
            errorTextView.setText(errorText);
        });

        // FIXME
        bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuHome);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menuHome) {
                return true;
            } else if (itemId == R.id.menuSettings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.menuShare) {
                bottomNavigationView.setSelectedItemId(R.id.menuHome); // don't change
                final String shareTitle = getString(R.string.share_title);
                final String shareMessage = getString(R.string.share_message);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, shareTitle));
            }
            return false;
        });

        // hide
        disconnectedStateUiItems();
    }

    private void connectingStateUiItems() {
        statusTextView.setText("Connecting...");
        spinnerServers.setBackground(AppCompatResources.getDrawable(this, R.drawable.round_back_secondary_100));
        spinnerServers.setEnabled(false);
    }

    private void disconnectedStateUiItems() {
        statusTextView.setText("Disconnected");
        timerTextView.setText("00:00:00");
        downloadTextView.setText("01 Mb/s");
        uploadTextView.setText("0 Mb/s");
        startStopButton.setChecked(false);

        hideView(homeTextViewTimeLabel);
        hideView(downloadTextView);
        hideView(uploadTextView);
        hideView(timerTextView);
        hideView(homeDownloadImageView);
        hideView(homeUploadImageView);

        spinnerServers.setBackground(AppCompatResources.getDrawable(this, R.drawable.round_back_white10_20));
        spinnerServers.setEnabled(true);
    }

    private void connectedStateUiItems() {
        statusTextView.setText("Running");
        fptnViewModel.clearErrorTextMessage();
        startStopButton.setChecked(true);

        showView(homeTextViewTimeLabel);
        showView(downloadTextView);
        showView(uploadTextView);
        showView(timerTextView);
        showView(homeDownloadImageView);
        showView(homeUploadImageView);

        spinnerServers.setBackground(AppCompatResources.getDrawable(this, R.drawable.round_back_secondary_100));
        spinnerServers.setEnabled(false);
    }

    private void hideView(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private void showView(View view) {
        if (view != null) {
            view.setVisibility(View.VISIBLE);
        }
    }

    public void onClickToStartStop(View v) {
        if (fptnViewModel.getConnectionStateMutableLiveData().getValue() == ConnectionState.DISCONNECTED) {
            List<FptnServerDto> fptnServerDtoList = ((FptnServerAdapter) spinnerServers.getAdapter()).getFptnServerDtoList();
            if (fptnServerDtoList != null && !fptnServerDtoList.isEmpty()) {
                // todo: выбор наилучшего сервера. сейчас для простоты первый
                FptnServerDto server = fptnServerDtoList.get(1);
                int selectedPosition = spinnerServers.getSelectedItemPosition();
                if (selectedPosition > 1) {
                    server = fptnServerDtoList.get(selectedPosition);
                }

                //TODO: отойти от sharedPref - передавать всю инфу в intent
                final SharedPreferences prefs = getSharedPreferences(SharedPreferencesFields.NAME, MODE_PRIVATE);
                prefs.edit()
                        .putString(SharedPreferencesFields.SERVER_ADDRESS, server.getHost())
                        .putInt(SharedPreferencesFields.SERVER_PORT, server.getPort())
                        .putString(SharedPreferencesFields.USERNAME, server.getUsername())
                        .putString(SharedPreferencesFields.PASSWORD, server.getPassword())
                        .apply();
                // Запрос на предоставление приложению возможности запускать впн Intent != null, если приложению еще не предоставлялся доступ
                Intent intent = VpnService.prepare(HomeActivity.this);
                if (intent != null) {
                    // Запрос на предоставление приложению возможности запускать впн
                    startActivityForResult(intent, 0);
                } else {
                    // Запрос был ранее предоставлен - сразу вызов onActivityResult с RESULT_OK
                    onActivityResult(0, RESULT_OK, null);
                }
            } else {
                Toast.makeText(this, "Server list is empty! Please login!", Toast.LENGTH_SHORT).show();
            }
        } else if (fptnViewModel.getConnectionStateMutableLiveData().getValue() == ConnectionState.CONNECTED) {
            startService(getServiceIntent().setAction(CustomVpnService.ACTION_DISCONNECT));
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result == RESULT_OK) {
            startService(getServiceIntent().setAction(CustomVpnService.ACTION_CONNECT));
        }
    }

    private Intent getServiceIntent() {
        return new Intent(this, CustomVpnService.class);
    }

}
