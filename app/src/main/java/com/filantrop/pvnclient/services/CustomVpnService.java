package com.filantrop.pvnclient.services;

import static com.filantrop.pvnclient.enums.IntentMessageType.SPEED_DOWNLOAD;
import static com.filantrop.pvnclient.enums.IntentMessageType.SPEED_UPLOAD;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.enums.HandlerMessageTypes;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;
import com.filantrop.pvnclient.views.HomeActivity;
import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.services.exception.PVNClientException;
import com.filantrop.pvnclient.enums.SharedPreferencesFields;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Getter;
import lombok.Setter;

public class CustomVpnService extends VpnService implements Handler.Callback {
    public static final String ACTION_CONNECT = "com.example.android.fptn.START";
    public static final String ACTION_DISCONNECT = "com.example.android.fptn.STOP";
    private final String TAG = this.getClass().getName();

    //Handler - очередь обрабатываемых в потоке сообщений.
    @Getter
    private Handler mHandler;

    @Getter
    private boolean isRunning = false;

    private int lastWhat;

    @Setter
    private FptnServerViewModel fptnViewModel;

    public void updateConnectionState() {
        if (fptnViewModel != null) {
            if (lastWhat == R.string.connected) {
                fptnViewModel.getConnectionStateMutableLiveData().postValue(ConnectionState.CONNECTED);
            }
            if (lastWhat == R.string.connecting) {
                fptnViewModel.getConnectionStateMutableLiveData().postValue(ConnectionState.CONNECTING);
            }
            if (lastWhat == R.string.disconnected) {
                fptnViewModel.getConnectionStateMutableLiveData().postValue(ConnectionState.DISCONNECTED);
            }
        }
    }

    private static class Connection extends Pair<Thread, ParcelFileDescriptor> {
        public Connection(Thread thread, ParcelFileDescriptor pfd) {
            super(thread, pfd);
        }
    }

    // Подключающийся поток. Ссылка обнуляется если подключение успешно.
    // Видимо, чтобы потушить поток с неудавшимся соединением
    private final AtomicReference<Thread> mConnectingThread = new AtomicReference<>();

    // Удачно подключившийся поток становится mConnection
    private final AtomicReference<Connection> mConnection = new AtomicReference<>();

    private final AtomicInteger mNextConnectionId = new AtomicInteger(1);

    // Отложенный Intent для запуска из нотификации Activity для конфигурации VPN
    private PendingIntent mConfigureIntent;

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
        //todo: вот тут из интента брать данные для подключения
        if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
            isRunning = false;
            disconnect();
            return START_NOT_STICKY;
        } else {
            isRunning = true;
            connect();
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        disconnect();
    }

    @Override
    public boolean handleMessage(Message message) {
        if (Objects.equals(HandlerMessageTypes.SPEED_UPLOAD.getValue(), message.what)) {
            if (fptnViewModel != null) {
                fptnViewModel.getUploadSpeedAsStringLiveData().postValue((String) message.obj);
            }
            return true;
        }
        if (Objects.equals(HandlerMessageTypes.SPEED_DOWNLOAD.getValue(), message.what)) {
            if (fptnViewModel != null) {
                fptnViewModel.getDownloadSpeedAsStringLiveData().postValue((String) message.obj);
            }
            return true;
        }

        // То раздражающее всплывающее сообщение
        // Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();

        lastWhat = message.what;
        updateConnectionState();

        if (message.what != R.string.disconnected) {
            updateForegroundNotification(message.what);
        }
        return true;
    }

    private void connect() {
        // Переводим VPNService на передний план - чтобы повысить приоритет
        updateForegroundNotification(R.string.connecting);
        mHandler.sendEmptyMessage(R.string.connecting);

        // Достаем параметры для подключения из SharedPreferences
        //TODO: отойти от sharedPref - передавать всю инфу в intent
        final SharedPreferences prefs = getSharedPreferences(SharedPreferencesFields.NAME, MODE_PRIVATE);
        final String server = prefs.getString(SharedPreferencesFields.SERVER_ADDRESS, "");
        final String username = prefs.getString(SharedPreferencesFields.USERNAME, "");
        final String password = prefs.getString(SharedPreferencesFields.PASSWORD, "");

        final int port = prefs.getInt(SharedPreferencesFields.SERVER_PORT, 0);
        try {
            startConnection(new CustomVpnConnection(
                    this, mNextConnectionId.getAndIncrement(), server, port, username, password));
        } catch (PVNClientException e) {
            mHandler.sendEmptyMessage(R.string.error);
        }
    }

    private void startConnection(final CustomVpnConnection connection) {
        // Replace any existing connecting thread with the  new one.
        final Thread thread = new Thread(connection, "ToyVpnThread");
        setConnectingThread(thread);

        // Handler to mark as connected once onEstablish is called.
        connection.setConfigureIntent(mConfigureIntent);
        connection.setOnEstablishListener(tunInterface -> {
            if (isRunning) {
                // Этот вызов из потока соединения вызывается
                mHandler.sendEmptyMessage(R.string.connected);

                mConnectingThread.compareAndSet(thread, null);
                setConnection(new Connection(thread, tunInterface));
            } else {
                connection.stop();
                thread.interrupt();
            }
        });
        thread.start();
    }

    private void setConnectingThread(final Thread thread) {
        final Thread oldThread = mConnectingThread.getAndSet(thread);
        if (oldThread != null) {
            oldThread.interrupt();
        }
    }

    private void setConnection(final Connection connection) {
        final Connection oldConnection = mConnection.getAndSet(connection);
        if (oldConnection != null) {
            try {
                oldConnection.first.interrupt();
                oldConnection.second.close();
            } catch (IOException e) {
                Log.e(getTag(), "Closing VPN interface", e);
            }
        }
    }

    private void disconnect() {
        mHandler.sendEmptyMessage(R.string.disconnected);
        setConnectingThread(null);
        setConnection(null);
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

    private String getTag() {
        return this.getClass().getCanonicalName();
    }
}
