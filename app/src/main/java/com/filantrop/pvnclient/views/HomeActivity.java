package com.filantrop.pvnclient.views;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServer;
import com.filantrop.pvnclient.repository.FptnServerAdaptor;
import com.filantrop.pvnclient.services.CustomVpnService;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

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
    private void intializeVariable() {
        connectionStatus = ConnectionStatus.NONE;
        adaptor = new FptnServerAdaptor(this, fptnServerList);
        adaptor.setFptnServerList(fptnServerList);

        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        fptnViewModel.getAllServersLiveData().observe(this, new Observer<List<FptnServer>>() {
            @Override
            public void onChanged(List<FptnServer> servers) {
                if(servers != null && !servers.isEmpty()) {
                    fptnServerList = servers;
                    adaptor.setFptnServerList(fptnServerList);
                }
            }
        });
        recyclerView = findViewById(R.id.servers_list_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerView.setAdapter(adaptor);
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
                recyclerView.setVisibility(View.GONE); // HIDE
            } else {
                Toast.makeText(this, "Server list is empty! Please login!", Toast.LENGTH_SHORT).show();
            }
        } else if (connectionStatus == ConnectionStatus.CONNECTED) {
            startService(getServiceIntent().setAction(CustomVpnService.ACTION_DISCONNECT));
            connectionStatus = ConnectionStatus.NONE;
            recyclerView.setVisibility(View.VISIBLE); // SHOW
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
