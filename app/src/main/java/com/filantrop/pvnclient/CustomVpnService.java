package com.filantrop.pvnclient;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.filantrop.pvnclient.exception.PVNClientException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CustomVpnService extends VpnService implements Handler.Callback {
    public static final String ACTION_CONNECT = "com.example.android.toyvpn.START";
    public static final String ACTION_DISCONNECT = "com.example.android.toyvpn.STOP";

    //Handler - очередь обрабатываемых в потоке сообщений.
    private Handler mHandler;

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

    @Override
    public void onCreate() {
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Создаем PendingIntent (Отложенное намерение) чтобы в Нотификации
        // была доступна кнопка конфигурации (при нажатии вызовет MainActivity)
        mConfigureIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_MUTABLE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
            disconnect();
            return START_NOT_STICKY;
        } else {
            connect();
            return START_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        disconnect();
    }

    @Override
    public boolean handleMessage(Message message) {
        Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
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
        final SharedPreferences prefs = getSharedPreferences(MainActivity.Prefs.NAME, MODE_PRIVATE);
        final String server = prefs.getString(MainActivity.Prefs.SERVER_ADDRESS, "");
        final String username = prefs.getString(MainActivity.Prefs.USERNAME, "");
        final String password = prefs.getString(MainActivity.Prefs.PASSWORD, "");

        final int port = prefs.getInt(MainActivity.Prefs.SERVER_PORT, 0);
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
            // Вот для этого и нужен handler, чтобы из потока соединения присылать на UI сообщения
            mHandler.sendEmptyMessage(R.string.connected);

            mConnectingThread.compareAndSet(thread, null);
            setConnection(new Connection(thread, tunInterface));
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
        final String NOTIFICATION_CHANNEL_ID = "ToyVpn";
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        mNotificationManager.createNotificationChannel(new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT));
        startForeground(1, new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_vpn)
                .setContentText(getString(message))
                .setContentIntent(mConfigureIntent)
                .build());
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }
}
