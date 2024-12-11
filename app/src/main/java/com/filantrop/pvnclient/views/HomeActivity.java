package com.filantrop.pvnclient.views;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServer;
import com.filantrop.pvnclient.repository.FptnServerAdaptor;
import com.filantrop.pvnclient.services.CustomVpnService;
import com.filantrop.pvnclient.utils.CountUpTimer;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class HomeActivity extends AppCompatActivity {

    public static String MSG_INTENT_FILTER = "fptn_home_activity";

    public static String MG_TYPE = "type";
    public static String MSG_PAYLOAD = "payload";

    public static String MSG_TYPE_CONNECTING = "connecting";
    public static String MSG_TYPE_CONNECTED_SUCCESS = "connected_success";
    public static String MSG_TYPE_CONNECTED_FAILED = "connected_failed";
    public static String MSG_TYPE_DISSCONNECTED = "dicconnected";
    public static String MSG_TYPE_SPEED_DOWNLOAD = "speed_download";
    public static String MSG_TYPE_SPEED_UPLOAD = "speed_upload";

    private enum ConnectionState {
        NONE,
        CONNECTING,
        CONNECTED;
    }
    public interface Prefs {
        String NAME = "connection";
        String SERVER_ADDRESS = "server.address";
        String SERVER_PORT = "server.port";
        String USERNAME = "username";
        String PASSWORD = "password";
    }


    private ConnectionState connectionState;

    private RecyclerView recyclerView;

    private AutoCompleteTextView autoCompleteTextView;
    private TextView downloadTextView;
    private TextView statusTextView;
    private TextView uploadTextView;

    private TextView homeTextViewTimeLabel;
    private TextView timerTextView;

    private CountUpTimer timer;

    private Spinner spinnerServers;

    private View homeDownloadImageView;
    private View homeUploadImageView;

    private FptnServerAdaptor adaptor;
    private List<FptnServer> fptnServerList;
    private FptnServerViewModel fptnViewModel = null;

    View serverListView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_layout);
        intializeVariable();
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
    private void intializeVariable() {
        connectionState = ConnectionState.NONE;
        adaptor = new FptnServerAdaptor(HomeActivity.this, fptnServerList);
//        adaptor.setFptnServerList(fptnServerList);

        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        fptnViewModel.getAllServersLiveData().observe(this, new Observer<List<FptnServer>>() {
            @Override
            public void onChanged(List<FptnServer> servers) {
                List<FptnServer> newServers = new ArrayList<>();
                if (servers != null) {
                    newServers.add(new FptnServer("Auto", "Auto", "Auto", "", 0));
                    newServers.addAll(servers);
                }
                if(servers != null && !servers.isEmpty()) {
                    fptnServerList = newServers ;
                    adaptor.setFptnServerList(fptnServerList);
                }
            }
        });

        homeTextViewTimeLabel = findViewById(R.id.homeTextViewTimeLabel);
        spinnerServers = findViewById(R.id.home_server_spinner);
        spinnerServers.setAdapter((SpinnerAdapter) adaptor);

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
        if (connectionState == ConnectionState.NONE) {
            if (!fptnServerList.isEmpty()) {
                final FptnServer server = fptnServerList.get(1); // first for DEMO
                final SharedPreferences prefs = getSharedPreferences(HomeActivity.Prefs.NAME, MODE_PRIVATE);
                prefs.edit()
                        .putString(HomeActivity.Prefs.SERVER_ADDRESS, server.getHost())
                        .putInt(HomeActivity.Prefs.SERVER_PORT, server.getPort())
                        .putString(HomeActivity.Prefs.USERNAME, server.getUsername())
                        .putString(HomeActivity.Prefs.PASSWORD, server.getPassword())
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
        } else if (connectionState == ConnectionState.CONNECTED) {
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

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TextView status = findViewById(R.id.homeTextViewConnectionStatus);

            String msgType = intent.getStringExtra(MG_TYPE);
            String msgPayload = intent.getStringExtra(MSG_PAYLOAD);

            if (msgType.equals(MSG_TYPE_CONNECTING)) {
                status.setText("Connecting...");
                connectionState = ConnectionState.CONNECTING;
            } else if (msgType.equals(MSG_TYPE_CONNECTED_SUCCESS)) {
                // show
                connectionState = ConnectionState.CONNECTED;
                showRunningUiItems();
                status.setText("Running");
                startTimer();
            } else if (msgType.equals(MSG_TYPE_CONNECTED_FAILED)) {
                status.setText("Fail: " + msgPayload);
                connectionState = ConnectionState.NONE;
                status.setText("Disconnected");
                hideRunningUiItems();
            } else if (msgType.equals(MSG_TYPE_DISSCONNECTED)) {
                connectionState = ConnectionState.NONE;
                status.setText("Disconnected");
                hideRunningUiItems();
                stopTimer();
            } else if (msgType.equals(MSG_TYPE_SPEED_DOWNLOAD) && connectionState == ConnectionState.CONNECTED) {
                downloadTextView.setText(msgPayload);
            } else if (msgType.equals(MSG_TYPE_SPEED_UPLOAD) && connectionState == ConnectionState.CONNECTED) {
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
