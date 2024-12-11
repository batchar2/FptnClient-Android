package com.filantrop.pvnclient.views;

import static com.filantrop.pvnclient.enums.IntentFields.MSG_PAYLOAD;
import static com.filantrop.pvnclient.enums.IntentFields.MSG_TYPE;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.enums.IntentMessageType;
import com.filantrop.pvnclient.enums.SharedPreferencesFields;
import com.filantrop.pvnclient.repository.FptnServerAdapter;
import com.filantrop.pvnclient.services.CustomVpnService;
import com.filantrop.pvnclient.utils.CountUpTimer;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class HomeActivity extends AppCompatActivity {
    public static String MSG_INTENT_FILTER = "fptn_home_activity";

    private FptnServerViewModel fptnViewModel;

    private TextView downloadTextView;
    private TextView uploadTextView;

    private TextView homeTextViewTimeLabel;
    private TextView timerTextView;

    private CountUpTimer timer;

    private View homeDownloadImageView;
    private View homeUploadImageView;

    private Spinner spinnerServers;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);

        initializeVariable();
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter(MSG_INTENT_FILTER));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(messageReceiver);
    }

    @SuppressLint("InlinedApi")
    private void initializeVariable() {
        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        fptnViewModel.setConnectionState(ConnectionState.NONE); // todo: статус подключения надо получать из сервиса!

        fptnViewModel.getAllServersLiveData().observe(this, servers -> {
            if (servers != null && !servers.isEmpty()) {
                List<FptnServerDto> fixedServers = new ArrayList<>();
                fixedServers.add(new FptnServerDto("Auto", "Auto", "Auto", "", 0));
                fixedServers.addAll(servers);
                ((FptnServerAdapter) spinnerServers.getAdapter()).setFptnServerDtoList(fixedServers);
            }
        });

        spinnerServers = findViewById(R.id.home_server_spinner);
        spinnerServers.setAdapter(new FptnServerAdapter());

        homeTextViewTimeLabel = findViewById(R.id.homeTextViewTimeLabel);
        downloadTextView = findViewById(R.id.downloadTextView);
        homeDownloadImageView = findViewById(R.id.homeDownloadImageView);
        uploadTextView = findViewById(R.id.uploadTextView);
        homeUploadImageView = findViewById(R.id.homeUploadImageView);
        homeDownloadImageView = findViewById(R.id.homeDownloadImageView);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter(MSG_INTENT_FILTER));

        timerTextView = findViewById(R.id.homeTextViewTime);

        // hide
        hideRunningUiItems();
    }


    private void hideRunningUiItems() {
        hideView(homeTextViewTimeLabel);
        hideView(downloadTextView);
        hideView(uploadTextView);
        hideView(timerTextView);
        hideView(homeDownloadImageView);
        hideView(homeUploadImageView);

        timerTextView.setText("00:00:00");
        downloadTextView.setText("0 Mb/s");
        uploadTextView.setText("0 Mb/s");
    }

    private void showRunningUiItems() {
        showView(homeTextViewTimeLabel);
        showView(downloadTextView);
        showView(uploadTextView);
        showView(timerTextView);
        showView(homeDownloadImageView);
        showView(homeUploadImageView);
    }

    public void onClickToStartStop(View v) {
        if (fptnViewModel.getConnectionState() == ConnectionState.NONE) {
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

//                connectionState = ConnectionState.CONNECTED;
            } else {
                Toast.makeText(this, "Server list is empty! Please login!", Toast.LENGTH_SHORT).show();
            }
        } else if (fptnViewModel.getConnectionState() == ConnectionState.CONNECTED) {
            stopTimer();
            startService(getServiceIntent().setAction(CustomVpnService.ACTION_DISCONNECT));
//            connectionState = ConnectionState.NONE;
//            showView(recyclerView); // SHOW
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

    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView status = findViewById(R.id.homeTextViewConnectionStatus);

            String msgTypeAsString = intent.getStringExtra(MSG_TYPE);
            String msgPayload = intent.getStringExtra(MSG_PAYLOAD);

            // todo: переделать на state machine. ну или хотя бы переделать на switch а лучше на enum
            IntentMessageType msgType = IntentMessageType.valueOf(msgTypeAsString);
            if (msgType.equals(IntentMessageType.CONNECTING)) {
                status.setText("Connecting...");
                fptnViewModel.setConnectionState(ConnectionState.CONNECTING);
            } else if (msgType.equals(IntentMessageType.CONNECTED_SUCCESS)) {
                // show
                fptnViewModel.setConnectionState(ConnectionState.CONNECTED);
                showRunningUiItems();
                status.setText("Running");
                startTimer();
            } else if (msgType.equals(IntentMessageType.CONNECTED_FAILED)) {
                status.setText("Fail: " + msgPayload);
                fptnViewModel.setConnectionState(ConnectionState.NONE);
                status.setText("Disconnected");
                hideRunningUiItems();
            } else if (msgType.equals(IntentMessageType.DISCONNECTED)) {
                fptnViewModel.setConnectionState(ConnectionState.NONE);
                status.setText("Disconnected");
                hideRunningUiItems();
                stopTimer();
            } else if (msgType.equals(IntentMessageType.SPEED_DOWNLOAD) && fptnViewModel.getConnectionState() == ConnectionState.CONNECTED) {
                downloadTextView.setText(msgPayload);
            } else if (msgType.equals(IntentMessageType.SPEED_UPLOAD) && fptnViewModel.getConnectionState() == ConnectionState.CONNECTED) {
                uploadTextView.setText(msgPayload);
            }
        }
    };

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

    private void startTimer() {
        long duration = 3600000 * 24 * 7; // 7 days FIXME
        if (timer == null) {
            timer = new CountUpTimer() {
                @Override
                public void onTick(int second) {
                    int hours = second / 3600;
                    int minutes = (second % 3600) / 60;
                    int seconds = second % 60;

                    // Format and display the time as HH:MM:SS
                    String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                    timerTextView.setText(time);
                }

                @Override
                public void onFinish() {
                    timerTextView.setText("00:00:00");
                }
            };
            timer.start();
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = null;
    }
}
