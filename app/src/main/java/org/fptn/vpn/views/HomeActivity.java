package org.fptn.vpn.views;

import static org.fptn.vpn.core.common.Constants.SELECTED_SERVER;
import static org.fptn.vpn.core.common.Constants.SELECTED_SERVER_ID_AUTO;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.utils.CustomSpinner;
import org.fptn.vpn.views.adapter.FptnServerAdapter;
import org.fptn.vpn.services.CustomVpnService;
import org.fptn.vpn.viewmodel.FptnServerViewModel;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.Getter;

public class HomeActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    @Getter
    private FptnServerViewModel fptnViewModel;

    private TextView connectionTimerTextView;

    private TextView downloadTextView;
    private TextView uploadTextView;

    private TextView statusTextView;
    private TextView errorTextView;

    private TextView connectedServerTextView;

    private View connectionTimeFrame;
    private View serverInfoFrame;
    private View homeSpeedFrame;
    private View settingsMenuItem;

    private CustomSpinner spinnerServers;

    private ToggleButton startStopButton;

    //for service binding
    private ServiceConnection connection;
    private CustomVpnService vpnService;

    private final ActivityResultLauncher<Intent> intentActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), activityResult -> {
                if (activityResult != null && activityResult.getResultCode() == RESULT_OK) {
                    startService(enrichIntent(getServiceIntent()).setAction(CustomVpnService.ACTION_CONNECT));
                } else {
                    Toast.makeText(this, R.string.vpn_permission_warning, Toast.LENGTH_SHORT).show();
                    fptnViewModel.getErrorTextLiveData().postValue(getString(R.string.vpn_permission_warning));
                }
            });

    // On Android >= 13.0 we need to require permissions on notifications
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.i(TAG, "Notifications enabled!");
                } else {
                    Log.i(TAG, "Notifications disabled!");
                }
            }
    );

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
                vpnService.updateStateInViewModel();
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
        spinnerServers = findViewById(R.id.home_server_spinner);

        startStopButton = findViewById(R.id.home_do_connect_button);
        startStopButton.setOnClickListener(this::onClickToStartStop);

        downloadTextView = findViewById(R.id.home_download_speed);
        uploadTextView = findViewById(R.id.home_upload_speed);
        connectionTimerTextView = findViewById(R.id.home_connection_timer);
        connectedServerTextView = findViewById(R.id.home_connected_server_name);
        statusTextView = findViewById(R.id.home_connection_status);
        errorTextView = findViewById(R.id.home_error_text_view);
        settingsMenuItem = findViewById(R.id.menuSettings);

        /*View containers to hide*/
        homeSpeedFrame = findViewById(R.id.home_speed_frame);
        connectionTimeFrame = findViewById(R.id.home_connection_timer_frame);
        serverInfoFrame = findViewById(R.id.home_server_info_frame);

        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        fptnViewModel.getServerDtoListLiveData().observe(this, fptnServerDtos -> {
            if (fptnServerDtos != null && !fptnServerDtos.isEmpty()) {
                List<FptnServerDto> fixedServers = new ArrayList<>();
                fixedServers.add(FptnServerDto.AUTO);
                fixedServers.addAll(fptnServerDtos);
                FptnServerAdapter fptnServerAdapter = new FptnServerAdapter(fixedServers,
                        R.layout.home_list_recycler_server_item);
                spinnerServers.setAdapter(fptnServerAdapter);

                int i = 0;
                for (FptnServerDto fixedServer : fixedServers) {
                    if (fixedServer.isSelected) {
                        spinnerServers.setSelection(i);
                        connectedServerTextView.setText(fixedServer.getServerInfo());
                    }
                    i++;
                }

                spinnerServers.performClosedEvent(); // FIX SPINNER BACKGROUND
            } else {
                // goto Login activity
                Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
        fptnViewModel.getConnectionStateMutableLiveData().observe(this, connectionStateInstantPair -> {
            switch (connectionStateInstantPair.first) {
                case CONNECTED:
                    connectedStateUiItems();
                    break;
                case DISCONNECTED:
                    disconnectedStateUiItems();
                    break;
                default:
                    break;
            }
        });
        fptnViewModel.getDownloadSpeedAsStringLiveData().observe(this, downloadSpeed -> downloadTextView.setText(downloadSpeed));
        fptnViewModel.getUploadSpeedAsStringLiveData().observe(this, uploadSpeed -> uploadTextView.setText(uploadSpeed));
        fptnViewModel.getErrorTextLiveData().observe(this, errorCodeText -> errorTextView.setText(errorCodeText));

        fptnViewModel.getTimerTextLiveData().observe(this, timerText -> connectionTimerTextView.setText(timerText));
        fptnViewModel.getStatusTextLiveData().observe(this, statusText -> statusTextView.setText(statusText));

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuHome);
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, bottomNavigationView, R.id.menuHome));

        fptnViewModel.getActiveStateLiveData().observe(this, isActive -> {
            startStopButton.setChecked(isActive);
            spinnerServers.setEnabled(!isActive);
            settingsMenuItem.setEnabled(!isActive);
        });

        // hide
        disconnectedStateUiItems();

        checkAndRequestNotificationPermission();
    }

    private void disconnectedStateUiItems() {
        hideView(connectionTimeFrame);
        hideView(serverInfoFrame);
        hideView(homeSpeedFrame);

        showView(spinnerServers);
    }

    private void connectedStateUiItems() {
        showView(connectionTimeFrame);
        showView(serverInfoFrame);
        showView(homeSpeedFrame);

        hideView(spinnerServers);
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
        ConnectionState currentConnectionState = Optional.ofNullable(fptnViewModel.getConnectionStateMutableLiveData().getValue()).map(pair -> pair.first)
                .orElse(ConnectionState.DISCONNECTED);
        if (currentConnectionState == ConnectionState.DISCONNECTED) {
            Intent intent = VpnService.prepare(HomeActivity.this);
            if (intent != null) {
                // Request to user on launch vpn
                intentActivityResultLauncher.launch(intent);
            } else {
                //explicit assignment cause service may start slowly
                fptnViewModel.getConnectionStateMutableLiveData().postValue(Pair.create(ConnectionState.CONNECTING, Instant.now()));

                startService(enrichIntent(getServiceIntent()).setAction(CustomVpnService.ACTION_CONNECT));
            }
        } else {
            if (currentConnectionState.isActiveState()) {
                startService(getServiceIntent().setAction(CustomVpnService.ACTION_DISCONNECT));
            }
            //reset error text message if when trying reconnect press button
            fptnViewModel.setErrorMessage("");
        }
    }

    private Intent getServiceIntent() {
        return new Intent(this, CustomVpnService.class);
    }

    private Intent enrichIntent(Intent intent) {
        FptnServerDto selectedItem = (FptnServerDto) spinnerServers.getSelectedItem();

        return intent.putExtra(SELECTED_SERVER,
                Optional.ofNullable(selectedItem).map(s -> s.id).orElse(SELECTED_SERVER_ID_AUTO));
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // On Android >= 13.0 we need to require permissions on notifications
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                SharedPreferences sharedPreferences = getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
                boolean hasRequestedBefore = sharedPreferences.getBoolean(Constants.NOTIFICATION_PERMISSION_REQUESTED_SHARED_PREF_KEY, false);
                if (hasRequestedBefore) {
                    return;
                }
                // Permission is not granted, show a dialog to explain reason
                new AlertDialog.Builder(this)
                        .setTitle(R.string.notifications_request_title)
                        .setMessage(R.string.notifications_request_reason)
                        .setPositiveButton(R.string.grant, (dialog, which) -> {
                            // Request the permission
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                            sharedPreferences.edit().putBoolean(Constants.NOTIFICATION_PERMISSION_REQUESTED_SHARED_PREF_KEY, true).apply();
                        })
                        .setNegativeButton(R.string.deny, (dialog, which) -> {
                            Log.i(TAG, "Notifications denied!");
                            sharedPreferences.edit().putBoolean(Constants.NOTIFICATION_PERMISSION_REQUESTED_SHARED_PREF_KEY, true).apply();
                        })
                        .create()
                        .show();
            } else {
                Log.i(TAG, "Notifications already allowed!");
            }
        } else {
            // On Android < 13.0 notifications enabled by default
            Log.i(TAG, "No need to request notification!");
        }
    }

}
