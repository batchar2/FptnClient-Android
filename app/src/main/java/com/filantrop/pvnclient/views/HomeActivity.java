package com.filantrop.pvnclient.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServer;
import com.filantrop.pvnclient.repository.FptnServerAdaptor;
import com.filantrop.pvnclient.services.CustomVpnService;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    public static String MSG_INTENT_FILTER = "fptn_home_activity";

    public static String MG_TYPE = "type";
    public static String MSG_PAYLOAD = "payload";

    public static String MSG_TYPE_CONNECTION = "connecting";
    public static String MSG_TYPE_CONNECTED_SUCCESS = "connected_success";
    public static String MSG_TYPE_CONNECTED_FAILED = "connected_failed";
    public static String MSG_TYPE_DISSCONNECTED = "dicconnected";
    public static String MSG_TYPE_SPEED_DOWNLOAD = "speed_download";
    public static String MSG_TYPE_SPEED_UPLOAD = "speed_upload";

    private enum ConnectionStatus {
        NONE,
        CONNECTION,
        CONNECTED;
    }
    public interface Prefs {
        String NAME = "connection";
        String SERVER_ADDRESS = "server.address";
        String SERVER_PORT = "server.port";
        String USERNAME = "username";
        String PASSWORD = "password";
    }


    private ConnectionStatus connectionStatus;

    private RecyclerView recyclerView;

    private AutoCompleteTextView autoCompleteTextView;
    private View downloadTextView;
    private View statusTextView;
    private View uploadTextView;

    private Spinner spinnerServers;


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

    private void intializeVariable() {
        connectionStatus = ConnectionStatus.NONE;
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

        spinnerServers = findViewById(R.id.home_server_spinner);
        spinnerServers.setAdapter((SpinnerAdapter) adaptor);

        downloadTextView = findViewById(R.id.downloadTextView);
        uploadTextView = findViewById(R.id.uploadTextView);

//        hideView(downloadTextView);
//        hideView(statusTextView);
//        hideView(uploadTextView);
    }

    public void onClickToStartStop(View v) {
        if (connectionStatus == ConnectionStatus.NONE) {
            if (!fptnServerList.isEmpty()) {
                final FptnServer server = fptnServerList.get(0); // first for DEMO

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

                connectionStatus = ConnectionStatus.CONNECTED;
//                hideView(recyclerView); // HIDE
//                showView(statusTextView);
            } else {
                Toast.makeText(this, "Server list is empty! Please login!", Toast.LENGTH_SHORT).show();
            }
        } else if (connectionStatus == ConnectionStatus.CONNECTED) {
            startService(getServiceIntent().setAction(CustomVpnService.ACTION_DISCONNECT));
            connectionStatus = ConnectionStatus.NONE;
            showView(recyclerView); // SHOW
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
            String msgType = intent.getStringExtra(MG_TYPE);
            String msgPayload = intent.getStringExtra(MSG_PAYLOAD);
            if (msgType.equals(MSG_TYPE_CONNECTION)) {
                //
            } else if (msgType.equals(MSG_TYPE_CONNECTED_SUCCESS)) {

            } else if (msgType.equals(MSG_TYPE_CONNECTED_FAILED)) {

            } else if (msgType.equals(MSG_TYPE_DISSCONNECTED)) {

            } else if (msgType.equals(MSG_TYPE_SPEED_DOWNLOAD)) {

            } else if (msgType.equals(MSG_TYPE_SPEED_UPLOAD)) {

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
}
