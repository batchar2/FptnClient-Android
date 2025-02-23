package org.fptn.vpnclient.services;

import static org.fptn.vpnclient.core.common.Constants.SELECTED_SERVER;
import static org.fptn.vpnclient.core.common.Constants.SELECTED_SERVER_ID_AUTO;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.fptn.vpnclient.R;
import org.fptn.vpnclient.core.common.Constants;
import org.fptn.vpnclient.database.model.FptnServerDto;
import org.fptn.vpnclient.enums.ConnectionState;
import org.fptn.vpnclient.enums.HandlerMessageTypes;
import org.fptn.vpnclient.repository.FptnServerRepository;
import org.fptn.vpnclient.utils.NetworkMonitor;
import org.fptn.vpnclient.viewmodel.FptnServerViewModel;
import org.fptn.vpnclient.views.HomeActivity;
import org.fptn.vpnclient.views.speedtest.SpeedTestService;
import org.fptn.vpnclient.vpnclient.exception.ErrorCode;
import org.fptn.vpnclient.vpnclient.exception.PVNClientException;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
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

    // Thread in connecting. If connecting success become null
    private final AtomicReference<CustomVpnConnection> connectingThread = new AtomicReference<>();

    // A successfully connected thread becomes mConnection
    private final AtomicReference<Pair<CustomVpnConnection, ParcelFileDescriptor>> connection = new AtomicReference<>();

    private final AtomicInteger nextConnectionId = new AtomicInteger(1);

    // Pending Intent for launch MainActivity when notification tapped
    private PendingIntent launchMainActivityPendingIntent;

    // Pending Intent to disconnect from notification
    private PendingIntent disconnectPendingIntent;

    // Server for finding the best server (and check availability)
    private SpeedTestService speedTestService;

    private FptnServerRepository fptnServerRepository;

    // Binder given to clients.
    private final IBinder binder = new LocalBinder();

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

        try {
            speedTestService = new SpeedTestService();
        } catch (PVNClientException e) {
            Log.e(TAG, "onCreate(): " + e.getMessage(), e);
            throw new RuntimeException(e);
        }

        fptnServerRepository = new FptnServerRepository(getApplicationContext());
    }

    @SneakyThrows
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "CustomVpnService.onStartCommand: " + intent);
        if (ACTION_DISCONNECT.equals(intent.getAction())) {
            /* if we need disconnect */
            // reset selected
            fptnServerRepository.resetSelected();
            // stop running threads
            disconnect();
            return START_NOT_STICKY;
        }
        if (!NetworkMonitor.isOnline(this)){
            Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getErrorTextLiveData().postValue(ErrorCode.NO_ACTIVE_INTERNET_CONNECTIONS.getValue()));
            Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getConnectionStateMutableLiveData().postValue(ConnectionState.DISCONNECTED));
            return START_NOT_STICKY;
        }

        if (ACTION_CONNECT.equals(intent.getAction())) {
            int serverId = intent.getIntExtra(SELECTED_SERVER, SELECTED_SERVER_ID_AUTO);
            if (serverId == SELECTED_SERVER_ID_AUTO) {
                try {
                    List<FptnServerDto> fptnServerDtos = fptnServerRepository.getAllServersListFuture().get();
                    Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getConnectionStateMutableLiveData().postValue(ConnectionState.CONNECTING));
                    FptnServerDto server = speedTestService.findFastestServer(fptnServerDtos);
                    return connectToServer(server.id);
                } catch (PVNClientException e) {
                    Log.e(TAG, "onStartCommand: findFastestServer error! ", e);
                    /* We don't need to connect if all servers are unreachable */
                    fptnServerRepository.resetSelected().get();
                    Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getConnectionStateMutableLiveData().postValue(ConnectionState.DISCONNECTED));
                    Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getErrorTextLiveData().postValue(ErrorCode.ALL_SERVERS_UNREACHABLE.getValue()));
                    return START_NOT_STICKY;
                }
            } else {
                Log.i(TAG, "onStartCommand: connectToServer with id: " + serverId);
                return connectToServer(serverId);
            }
        } else {
            /* restart after service destruction because all fields of intent is null */
            Log.i(TAG, "onStartCommand: restart after error");
            return connectToPreviouslySelectedServer();
        }
    }

    private int connectToServer(int serverId) throws ExecutionException, InterruptedException {
        fptnServerRepository.resetSelected().get();
        fptnServerRepository.setIsSelected(serverId).get();

        FptnServerDto server = fptnServerRepository.getSelected().get();
        if (server != null) {
            connect(server);
            return START_STICKY;
        } else {
            /* selected server with selected id not found in DB - very */
            Log.e(TAG, "connectToServer: selected server not found in DB");
            Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getConnectionStateMutableLiveData().postValue(ConnectionState.DISCONNECTED));
            return START_NOT_STICKY;
        }
    }

    private int connectToPreviouslySelectedServer() throws ExecutionException, InterruptedException {
        FptnServerDto server = fptnServerRepository.getSelected().get();
        if (server != null) {
            connect(server);
            return START_STICKY;
        } else {
            /* selected server not selected - that should mean that we were disconnected correct previously */
            Log.i(TAG, "connectToPreviouslySelectedServer: previously selected server is null. No need to reconnect");
            Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getConnectionStateMutableLiveData().postValue(ConnectionState.DISCONNECTED));
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        disconnect();
    }

    public void updateConnectionStateInViewModel() {
        Optional.ofNullable(fptnViewModel).ifPresent(model -> {
            if (connection.get() != null) {
                model.getConnectionStateMutableLiveData().postValue(ConnectionState.CONNECTED);
                // send to UI startTime of active connection
                model.startTimer(connection.get().first.getConnectionTime());
            } else if (connection.get() == null && connectingThread.get() != null) {
                model.getConnectionStateMutableLiveData().postValue(ConnectionState.CONNECTING);
            } else if (connection.get() == null && connectingThread.get() == null) {
                model.getConnectionStateMutableLiveData().postValue(ConnectionState.DISCONNECTED);
            }
        });
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        Log.d(TAG, "handleMessage: " + message);

        HandlerMessageTypes type = Arrays.stream(HandlerMessageTypes.values()).filter(t -> t.getValue() == message.what).findFirst().orElse(HandlerMessageTypes.UNKNOWN);
        switch (type) {
            case SPEED_INFO:
                Optional.ofNullable(fptnViewModel).ifPresent(model -> {
                    Triple<String, String, String> downloadSpeedUploadSpeed = (Triple<String, String, String>) message.obj;
                    String downloadSpeed = downloadSpeedUploadSpeed.getFirst();
                    String uploadSpeed = downloadSpeedUploadSpeed.getSecond();
                    String serverInfo = downloadSpeedUploadSpeed.getThird();

                    model.getDownloadSpeedAsStringLiveData().postValue(downloadSpeed);
                    model.getUploadSpeedAsStringLiveData().postValue(uploadSpeed);
                    updateNotificationWithMessage("Connected to " + serverInfo, "Download: " + downloadSpeed + " Upload: " + uploadSpeed);
                });
                break;
            case CONNECTION_STATE:
                Optional.ofNullable(fptnViewModel).ifPresent(model -> {
                    Triple<ConnectionState, Instant, String> connectionStateInstantPair = (Triple<ConnectionState, Instant, String>) message.obj;
                    Optional<ConnectionState> connectionState = Arrays.stream(ConnectionState.values()).filter(t -> t == connectionStateInstantPair.getFirst()).findFirst();
                    connectionState.ifPresent(state -> {
                        model.getConnectionStateMutableLiveData().postValue(state);
                        if (ConnectionState.CONNECTED.equals(state)) {
                            model.startTimer(connectionStateInstantPair.getSecond());
                            updateNotificationWithMessage("Connected to " + connectionStateInstantPair.getThird(), "");
                        } else if (ConnectionState.DISCONNECTED.equals(state)) {
                            model.stopTimer();
                            disconnect();
                        }
                    });
                });
                break;
            case ERROR:
                Optional.ofNullable(fptnViewModel).ifPresent(model -> {
                    model.getErrorTextLiveData().postValue((String) message.obj);
                });
                break;
            default:
                Log.e(TAG, "unexpected message: " + message);
        }
        return true;
    }

    private void connect(FptnServerDto fptnServerDto) {
        // Moving VPNService to foreground to give it higher priority in system
        startForegroundWithNotification("Connecting to " + fptnServerDto.getServerInfo());

        CustomVpnConnection connection = new CustomVpnConnection(
                this, nextConnectionId.getAndIncrement(), fptnServerDto);
        setConnectingConnection(connection);

        // Handler to mark as connected once onEstablish is called.
        connection.setConfigureIntent(launchMainActivityPendingIntent);
        connection.setOnEstablishListener(tunInterface -> {
            // Если удалось подключиться, обнуляем подключающийся поток и
            // сохраняем пару подключившийся поток/tunInterface
            connectingThread.compareAndSet(connection, null);
            setEstablishedConnection(new Pair<>(connection, tunInterface));
        });
        connection.start();
    }

    private void setConnectingConnection(final Thread thread) {
        final Thread oldThread = connectingThread.getAndSet((CustomVpnConnection) thread);
        if (oldThread != null) {
            oldThread.interrupt();
        }
    }

    private void setEstablishedConnection(final Pair<CustomVpnConnection, ParcelFileDescriptor> establishedConnection) {
        final Pair<CustomVpnConnection, ParcelFileDescriptor> oldEstablishedConnection = connection.getAndSet(establishedConnection);
        if (oldEstablishedConnection != null) {
            try {
                // прерываем старый поток
                oldEstablishedConnection.first.interrupt();
                // закрываем старый интерфейс
                oldEstablishedConnection.second.close();
            } catch (IOException e) {
                Log.e(TAG, "Closing VPN interface", e);
            }
        }
    }

    private void disconnect() {
        // stop and null thread in connecting
        setConnectingConnection(null);
        // stop and null existed connection
        setEstablishedConnection(null);
        // remove service from foreground - and remove notification
        stopForeground(true);
        //send to UI activity that state is disconnected.
        Optional.ofNullable(fptnViewModel).ifPresent(model -> model.getConnectionStateMutableLiveData().postValue(ConnectionState.DISCONNECTED));
    }

    private void startForegroundWithNotification(String title) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(Constants.MAIN_NOTIFICATION_CHANNEL_ID);
        if (notificationChannel == null) {
            notificationManager.createNotificationChannelGroup(
                    new NotificationChannelGroup(Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID, getString(R.string.notification_group_name)));

            NotificationChannel newNotificationChannel = new NotificationChannel(
                    Constants.MAIN_NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            newNotificationChannel.setGroup(Constants.MAIN_NOTIFICATION_CHANNEL_GROUP_ID);
            notificationManager.createNotificationChannel(
                    newNotificationChannel
            );
        }

        Notification notification = createNotification(title, "");
        startForeground(Constants.MAIN_CONNECTED_NOTIFICATION_ID, notification);
    }

    private void updateNotificationWithMessage(String title, String message) {
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
}
