package org.fptn.vpn.services;

import static org.fptn.vpn.core.common.Constants.SELECTED_SERVER;
import static org.fptn.vpn.core.common.Constants.SELECTED_SERVER_ID_AUTO;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.enums.HandlerMessageTypes;
import org.fptn.vpn.repository.FptnServerRepository;
import org.fptn.vpn.utils.NetworkMonitor;
import org.fptn.vpn.utils.NotificationUtils;
import org.fptn.vpn.viewmodel.FptnServerViewModel;
import org.fptn.vpn.views.HomeActivity;
import org.fptn.vpn.views.speedtest.SpeedTestUtils;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.security.Provider;
import java.security.Security;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import kotlin.Triple;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

public class CustomVpnService extends VpnService implements Handler.Callback {
    private static final String TAG = CustomVpnService.class.getName();

    public static final String ACTION_CONNECT = "com.example.android.fptn.START";
    public static final String ACTION_DISCONNECT = "com.example.android.fptn.STOP";

    //Handler - queue of events from another threads, that need to process in this thread
    @Getter
    private Handler handler;

    @Setter
    private FptnServerViewModel fptnViewModel;

    private final AtomicReference<CustomVpnConnection> activeConnection = new AtomicReference<>();

    private final AtomicInteger nextConnectionId = new AtomicInteger(1);

    // Pending Intent for launch MainActivity when notification tapped
    private PendingIntent launchMainActivityPendingIntent;

    // Pending Intent to disconnect from notification
    private PendingIntent disconnectPendingIntent;

    private FptnServerRepository fptnServerRepository;

    // Binder given to clients.
    private final IBinder binder = new LocalBinder();

    private boolean isNotificationAllowed = false;

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public CustomVpnService getService() {
            return CustomVpnService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "CustomVpnService.onCreate: " + Thread.currentThread().getId());
        // The handler is only used to show messages.
        if (handler == null) {
            handler = new Handler(this);
        }
        // pending intent for open MainActivity on tap
        launchMainActivityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, HomeActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        // pending intent for disconnect button in notification
        disconnectPendingIntent = PendingIntent.getService(this, 0,
                new Intent(this, CustomVpnService.class)
                        .setAction(CustomVpnService.ACTION_DISCONNECT),
                PendingIntent.FLAG_IMMUTABLE);
        fptnServerRepository = new FptnServerRepository(getApplicationContext());
    }

    @NonNull
    private String getSniHostname() {
        SharedPreferences sharedPreferences = getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.CURRENT_SNI_SHARED_PREF_KEY, getString(R.string.default_sni));
    }

    @SneakyThrows
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "CustomVpnService.onStartCommand: " + intent);

        /* check if notification allowed */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        isNotificationAllowed = notificationManager.areNotificationsEnabled();

        /* check is internet connection available */
        if (!NetworkMonitor.isOnline(this)) {
            Log.e(TAG, "onStartCommand: no active internet connections!");
            setException(new PVNClientException(ErrorCode.NO_ACTIVE_INTERNET_CONNECTIONS));
            disconnect();
            return START_NOT_STICKY;
        }

        final String sniHostname = getSniHostname();
        if (intent == null) {
            /* restart after service destruction because all fields of intent is null */
            Log.w(TAG, "onStartCommand: restart after error");
            return connectToPreviouslySelectedServer(sniHostname);
        } else if (ACTION_DISCONNECT.equals(intent.getAction())) {
            Log.i(TAG, "onStartCommand: disconnect!");
            /* if we need disconnect */
            // reset selected - TODO: DOES WE NEED TO RESET SELECTED SERVER IF WE CONNECTED OK?
            fptnServerRepository.resetSelected();
            // stop running threads
            disconnect();
            return START_NOT_STICKY;
        } else if (ACTION_CONNECT.equals(intent.getAction())) {
            int serverId = intent.getIntExtra(SELECTED_SERVER, SELECTED_SERVER_ID_AUTO);
            if (serverId == SELECTED_SERVER_ID_AUTO) {
                try {
                    List<FptnServerDto> fptnServerDtos = fptnServerRepository.getServersListFuture(false).get();
                    setConnectionState(ConnectionState.CONNECTING);
                    FptnServerDto server = SpeedTestUtils.findFastestServer(fptnServerDtos, sniHostname);
                    return connectToServer(server.id, sniHostname);
                } catch (PVNClientException e) {
                    Log.e(TAG, "onStartCommand: findFastestServer error! ", e);
                    /* We don't need to connect if all servers are unreachable */
                    fptnServerRepository.resetSelected().get();
                    setConnectionState(ConnectionState.DISCONNECTED);
                    setException(e);
                    return START_NOT_STICKY;
                }
            } else {
                Log.i(TAG, "onStartCommand: connectToServer with id: " + serverId);
                return connectToServer(serverId, sniHostname);
            }
        } else {
            // should not happen
            Log.e(TAG, "onStartCommand: action not recognize!");
            setException(new PVNClientException("CustomVpnService.onStartCommand: action not recognize!"));
            return START_NOT_STICKY;
        }
    }

    private void setConnectionState(ConnectionState connectionState) {
        setConnectionState(connectionState.getWithTime());
    }

    private void setConnectionState(Pair<ConnectionState, Instant> connectionStateWithTime) {
        if (fptnViewModel != null) {
            fptnViewModel.getConnectionStateMutableLiveData().postValue(connectionStateWithTime);
        }
    }

    private void setStatusText(String message) {
        if (fptnViewModel != null) {
            fptnViewModel.getStatusTextLiveData().postValue(message);
        }
    }

    private void setException(PVNClientException exception) {
        if (fptnViewModel != null) {
            fptnViewModel.getLastExceptionLiveData().postValue(exception);
        }
    }

    private int connectToServer(int serverId, String sniHostname) throws ExecutionException, InterruptedException {
        fptnServerRepository.resetSelected().get();
        fptnServerRepository.setIsSelected(serverId).get();

        FptnServerDto server = fptnServerRepository.getSelected().get();
        if (server != null) {
            connect(server, sniHostname);
            return START_STICKY;
        } else {
            /* selected server with selected id not found in DB - very */
            Log.e(TAG, "connectToServer: selected server not found in DB");
            setConnectionState(ConnectionState.DISCONNECTED);
            return START_NOT_STICKY;
        }
    }

    private int connectToPreviouslySelectedServer(String sniHostname) throws ExecutionException, InterruptedException {
        FptnServerDto server = fptnServerRepository.getSelected().get();
        if (server != null) {
            connect(server, sniHostname);
            return START_STICKY;
        } else {
            /* selected server not selected - that should mean that we were disconnected correct previously */
            Log.i(TAG, "connectToPreviouslySelectedServer: previously selected server is null. No need to reconnect");
            setConnectionState(ConnectionState.DISCONNECTED);
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        disconnect();
    }

    public void updateStateInViewModel() {
        CustomVpnConnection vpnConnection = activeConnection.get();
        if (vpnConnection == null) {
            setConnectionState(ConnectionState.DISCONNECTED);
        } else {
            switchState(
                    vpnConnection.getCurrentConnectionState(),
                    vpnConnection.getFptnServerDto().getServerInfo(),
                    vpnConnection.getConnectionTime(),
                    vpnConnection.getReconnectCount().get()
            );
        }
    }

    private void switchState(ConnectionState connectionState, String serverInfo, Instant connectionTime, int reconnectCount) {
        switch (connectionState) {
            case DISCONNECTED -> disconnect();
            case CONNECTING -> {
                String title = getString(R.string.connecting_to) + serverInfo;

                setConnectionState(ConnectionState.CONNECTING);
                setStatusText(title);
            }
            case CONNECTED -> {
                String title_connected_to = getString(R.string.connected_to) + serverInfo;
                updateNotificationWithMessage(title_connected_to , "");

                setConnectionState(ConnectionState.CONNECTED.getWithTime(connectionTime));

                String title = getString(R.string.connected);
                setStatusText(title);
            }
            case RECONNECTING -> {
                String title = getString(R.string.reconnection_to) + serverInfo;
                String msg = getString(R.string.try_number) + reconnectCount;
                updateNotificationWithMessage(title, msg);

                setConnectionState(ConnectionState.RECONNECTING);
                setStatusText(title);
                setErrorMessage(msg);
            }
        }
    }

    private void updateSpeed(String downloadSpeed, String uploadSpeed, String serverInfo) {
        if (fptnViewModel != null) {
            fptnViewModel.updateSpeed(downloadSpeed, uploadSpeed);
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        Log.d(TAG, "handleMessage: " + message);

        /* Check connectionId in message to exclude false positive change state UI on dead connection */
        int connectionId = message.arg1;
        if (Optional.ofNullable(activeConnection.get()).map(CustomVpnConnection::getConnectionId).orElse(-1) == connectionId) {
            HandlerMessageTypes type = Arrays.stream(HandlerMessageTypes.values()).filter(t -> t.getValue() == message.what).findFirst().orElse(HandlerMessageTypes.UNKNOWN);
            switch (type) {
                case SPEED_INFO:
                    if (activeConnection.get().getCurrentConnectionState() == ConnectionState.CONNECTED) {
                        Triple<String, String, String> downloadSpeedUploadSpeed = (Triple<String, String, String>) message.obj;
                        String downloadSpeed = downloadSpeedUploadSpeed.getFirst();
                        String uploadSpeed = downloadSpeedUploadSpeed.getSecond();
                        String serverInfo = downloadSpeedUploadSpeed.getThird();

                        updateNotificationWithMessage(getString(R.string.connected_to) + serverInfo, String.format(getString(R.string.download_upload_speed_pattern), downloadSpeed, uploadSpeed));
                        updateSpeed(downloadSpeed, uploadSpeed, serverInfo);
                    }
                    break;
                case CONNECTION_STATE:
                    Triple<ConnectionState, Instant, String> connectionStateInstantPair = (Triple<ConnectionState, Instant, String>) message.obj;
                    ConnectionState connectionState = connectionStateInstantPair.getFirst();
                    Instant connectionTime = connectionStateInstantPair.getSecond();
                    String serverInfo = connectionStateInstantPair.getThird();
                    int reconnectionCount = message.arg2;

                    switchState(connectionState, serverInfo, connectionTime, reconnectionCount);
                    break;
                case ERROR:
                    PVNClientException exception = (PVNClientException) message.obj;
                    setException(exception);
                    if (Objects.equals(exception.errorCode, ErrorCode.RECONNECTING_FAILED)) {
                        showReconnectionFailedNotification();
                    }
                    break;
                default:
                    Log.e(TAG, "unexpected message: " + message);
            }
        }
        return true;
    }

    private void setErrorMessage(String message) {
        if (fptnViewModel != null) {
            fptnViewModel.setErrorMessage(message);
        }
    }

    private void connect(FptnServerDto fptnServerDto, String sniHostname) {
        // Moving VPNService to foreground to give it higher priority in system
        startForegroundWithNotification(getString(R.string.connecting_to) + fptnServerDto.getServerInfo());

        try {
            CustomVpnConnection connection = new CustomVpnConnection(
                    this, nextConnectionId.getAndIncrement(), fptnServerDto, sniHostname);
            connection.setConfigureVpnIntent(launchMainActivityPendingIntent);
            connection.start();

            setActiveConnection(connection);
        } catch (PVNClientException ex) {
            setException(ex);
            disconnect();
        }
    }

    private void setActiveConnection(CustomVpnConnection connection) {
        CustomVpnConnection oldConnection = activeConnection.getAndSet(connection);
        if (oldConnection != null) {
            oldConnection.shutdown();
        }
    }

    private void disconnect() {
        // stop and null existed connection
        setActiveConnection(null);
        // remove service from foreground - and remove notification
        stopForeground(true);
        // sometimes need to remove notification explicitly
        removeForegroundNotification();
        //send to UI activity that state is disconnected.
        setConnectionState(ConnectionState.DISCONNECTED);
    }

    private void removeForegroundNotification() {
        if (!isNotificationAllowed) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Constants.MAIN_CONNECTED_NOTIFICATION_ID);
    }

    private void startForegroundWithNotification(String title) {
        if (!isNotificationAllowed) {
            return;
        }

        NotificationUtils.configureNotificationChannel(this);
        Notification notification = createNotification(title, "");
        startForeground(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification);
    }

    private void updateNotificationWithMessage(String title, String message) {
        if (!isNotificationAllowed) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        Notification notification = createNotification(title, message);
        notificationManager.notify(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String title, String message) {
        // In Api level 24 an above, there is no icon in design!!!
        Notification.Action actionDisconnect = new Notification.Action.Builder(null, getString(R.string.disconnect_action), disconnectPendingIntent)
                .build();
        Notification.Builder builder = new Notification.Builder(this, Constants.MAIN_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.vpn_icon)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
            //.setOngoing(true) // user can't close notification (works only when screen locked)
        }
        builder.setContentTitle(title)
                .setContentText(message)
                .setOnlyAlertOnce(true) // so when data is updated don't make sound and alert in android 8.0+
                //.setAutoCancel(false) // for not remove notification after press it
                .addAction(actionDisconnect)
                .setContentIntent(launchMainActivityPendingIntent);
        return builder.build();
    }

    private void showReconnectionFailedNotification() {
        if (!isNotificationAllowed) {
            return;
        }

        Notification notification = new Notification.Builder(this, Constants.MAIN_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.vpn_icon)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentTitle(getApplication().getString(R.string.reconnecting_failed))
                //.setContentText(message)
                //.setAutoCancel(false) // for not remove notification after press it
                .setContentIntent(launchMainActivityPendingIntent)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.INFO_NOTIFICATION_NOTIFICATION_ID, notification);
    }
}
