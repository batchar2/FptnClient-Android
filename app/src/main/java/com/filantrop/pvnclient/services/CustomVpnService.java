package com.filantrop.pvnclient.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.enums.HandlerMessageTypes;
import com.filantrop.pvnclient.enums.IntentExtraFieldNames;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;
import com.filantrop.pvnclient.views.HomeActivity;
import com.filantrop.pvnclient.R;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.Setter;

public class CustomVpnService extends VpnService implements Handler.Callback {
    private static final String TAG = CustomVpnService.class.getName();

    public static final String ACTION_CONNECT = "com.example.android.fptn.START";
    public static final String ACTION_DISCONNECT = "com.example.android.fptn.STOP";

    //Handler - очередь обрабатываемых в потоке сообщений.
    @Getter
    private Handler mHandler;

    @Setter
    private FptnServerViewModel fptnViewModel;

    // Подключающийся поток. Ссылка обнуляется если подключение успешно.
    // Видимо, чтобы потушить поток с неудавшимся соединением
    private final AtomicReference<CustomVpnConnection> mConnectingThread = new AtomicReference<>();

    // Удачно подключившийся поток становится mConnection
    private final AtomicReference<EstablishedConnection> mConnection = new AtomicReference<>();

    private final AtomicInteger mNextConnectionId = new AtomicInteger(1);

    // Отложенный Intent для запуска из нотификации Activity для конфигурации VPN
    private PendingIntent mConfigureIntent;

    // Binder given to clients.
    private final IBinder binder = new LocalBinder();
    private FptnServerDto selectedServer;

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
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Создаем PendingIntent (Отложенное намерение) чтобы в Нотификации
        // была доступна кнопка конфигурации (при нажатии вызовет MainActivity)
        mConfigureIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, HomeActivity.class),
                PendingIntent.FLAG_MUTABLE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "CustomVpnService.onStartCommand: " + intent.getAction());
        if (ACTION_DISCONNECT.equals(intent.getAction())) {
            disconnect();
            return START_NOT_STICKY;
        } else {
            // Достаем параметры для подключения из intent
            selectedServer = (FptnServerDto) intent.getSerializableExtra(IntentExtraFieldNames.SELECTED_SERVER);
            connect(selectedServer.getHost(), selectedServer.getPort(), selectedServer.getUsername(), selectedServer.getPassword());
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        disconnect();
    }


    public void updateConnectionStateInViewModel() {
        if (mConnection.get() != null) {
            fptnViewModel.getConnectionStateMutableLiveData().postValue(ConnectionState.CONNECTED);
            //Передаем на UI актуальные параметры подключения
            fptnViewModel.startTimer(mConnection.get().first.getConnectionTime());
            fptnViewModel.getSelectedServerLiveData().postValue(selectedServer);
        } else if (mConnection.get() == null && mConnectingThread.get() != null) {
            fptnViewModel.getConnectionStateMutableLiveData().postValue(ConnectionState.CONNECTING);
        } else if (mConnection.get() == null && mConnectingThread.get() == null) {
            fptnViewModel.getConnectionStateMutableLiveData().postValue(ConnectionState.DISCONNECTED);
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        Log.d(TAG, "handleMessage: " + message);

        HandlerMessageTypes type = Arrays.stream(HandlerMessageTypes.values()).filter(t -> t.getValue() == message.what).findFirst().orElse(HandlerMessageTypes.UNKNOWN);
        switch (type) {
            case SPEED_INFO:
                if (fptnViewModel != null) {
                    Pair<String, String> downloadSpeedUploadSpeed = (Pair<String, String>) message.obj;
                    fptnViewModel.getDownloadSpeedAsStringLiveData().postValue(downloadSpeedUploadSpeed.first);
                    fptnViewModel.getUploadSpeedAsStringLiveData().postValue(downloadSpeedUploadSpeed.second);
                }
                break;
            case CONNECTION_STATE:
                if (fptnViewModel != null) {
                    Pair<ConnectionState, Instant> connectionStateInstantPair = (Pair<ConnectionState, Instant>) message.obj;
                    Optional<ConnectionState> connectionState = Arrays.stream(ConnectionState.values()).filter(t -> t == connectionStateInstantPair.first).findFirst();
                    connectionState.ifPresent(state -> {
                        fptnViewModel.getConnectionStateMutableLiveData().postValue(state);
                        if (ConnectionState.CONNECTED.equals(state)) {
                            fptnViewModel.startTimer(connectionStateInstantPair.second);
                        } else if (ConnectionState.DISCONNECTED.equals(state)) {
                            fptnViewModel.stopTimer();
                            disconnect();
                        }
                    });
                }
                break;
            case ERROR:
                fptnViewModel.getErrorTextLiveData().postValue((String) message.obj);
                break;
            default:
                Log.e(TAG, "unexpected message: " + message);
        }
        return true;
    }

    private void connect(String server, int port, String username, String password) {
        // Переводим VPNService на передний план - чтобы повысить приоритет
        updateForegroundNotification(R.string.connecting);

        CustomVpnConnection connection = new CustomVpnConnection(
                this, mNextConnectionId.getAndIncrement(), server, port, username, password);
        setConnectingConnection(connection);

        // Handler to mark as connected once onEstablish is called.
        connection.setConfigureIntent(mConfigureIntent);
        connection.setOnEstablishListener(tunInterface -> {
            // Если удалось подключиться, обнуляем подключающийся поток и
            // сохраняем пару подключившийся поток/tunInterface
            mConnectingThread.compareAndSet(connection, null);
            setEstablishedConnection(new EstablishedConnection(connection, tunInterface));
        });
        connection.start();
    }

    private void setConnectingConnection(final Thread thread) {
        final Thread oldThread = mConnectingThread.getAndSet((CustomVpnConnection) thread);
        if (oldThread != null) {
            oldThread.interrupt();
        }
    }

    private void setEstablishedConnection(final EstablishedConnection establishedConnection) {
        final EstablishedConnection oldEstablishedConnection = mConnection.getAndSet(establishedConnection);
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
        // прерываем/обнуляем подключающийся поток (если есть)
        setConnectingConnection(null);
        // прерываем/обнуляем установленное соединение
        setEstablishedConnection(null);
        // выводим сервис из переднего плана
        stopForeground(true);
    }

    // Выводит в уведомления
    private void updateForegroundNotification(final int message) {
        final String NOTIFICATION_CHANNEL_ID = "FptnVPN";
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        mNotificationManager.createNotificationChannel(new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_HIGH));
        Notification notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.vpn_icon)
                .setContentText(getString(message))
                .setContentIntent(mConfigureIntent)
                .build();

        // айдишник по идее должен быть уникальным,
        // но пусть будет так - потому что только одна нотификация - пока сервис работает!
        startForeground(1, notification);
    }

}
