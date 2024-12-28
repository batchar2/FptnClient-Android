package com.filantrop.pvnclient.services;

import static com.filantrop.pvnclient.enums.IntentFields.MSG_PAYLOAD;
import static com.filantrop.pvnclient.enums.IntentFields.MSG_TYPE;
import static com.filantrop.pvnclient.enums.IntentMessageType.CONNECTED_FAILED;
import static com.filantrop.pvnclient.enums.IntentMessageType.CONNECTED_SUCCESS;
import static com.filantrop.pvnclient.enums.IntentMessageType.CONNECTING;
import static com.filantrop.pvnclient.enums.IntentMessageType.DISCONNECTED;
import static com.filantrop.pvnclient.enums.IntentMessageType.SPEED_DOWNLOAD;
import static com.filantrop.pvnclient.enums.IntentMessageType.SPEED_UPLOAD;
import static com.filantrop.pvnclient.views.HomeActivity.MSG_INTENT_FILTER;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.IpPrefix;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.filantrop.pvnclient.enums.IntentMessageType;
import com.filantrop.pvnclient.services.exception.PVNClientException;
import com.filantrop.pvnclient.services.websocket.CustomWebSocketListener;
import com.filantrop.pvnclient.services.websocket.OkHttpClientWrapper;
import com.filantrop.pvnclient.services.websocket.WebSocketMessageCallback;
import com.filantrop.pvnclient.utils.DataRateCalculator;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CustomVpnConnection implements Runnable {
    /**
     * Callback interface to let the {@link CustomVpnService} know about new connections
     * and update the foreground notification with connection status.
     */
    public interface OnEstablishListener {
        void onEstablish(ParcelFileDescriptor tunInterface);
    }

    /**
     * Maximum packet size is constrained by the MTU
     */
    private static final int MAX_PACKET_SIZE = 65536private final CustomVpnService service;
    private final int connectionId;

    private final String serverHost;
    private final int serverPort;

    private final OkHttpClientWrapper okHttpClientWrapper;

    private PendingIntent mConfigureIntent;
    private OnEstablishListener mOnEstablishListener;

    private static AtomicBoolean isRunning = new AtomicBoolean(false);

    private DataRateCalculator downloadRate;
    private DataRateCalculator uploadRate;

    private ScheduledExecutorService scheduler;


    public CustomVpnConnection(final CustomVpnService service, final int connectionId,
                               final String serverHost, final int serverPort,
                               final String username, final String password) {
        isRunning.set(true);
        this.service = service;
        this.connectionId = connectionId;

        this.serverHost = serverHost;
        this.serverPort = serverPort;

        this.okHttpClientWrapper = new OkHttpClientWrapper(service, username, password, serverHost, serverPort);

        this.downloadRate = new DataRateCalculator(1000);
        this.uploadRate = new DataRateCalculator(1000);
    }

    /**
     * Optionally, set an intent to configure the VPN. This is {@code null} by default.
     */
    public void setConfigureIntent(PendingIntent intent) {
        mConfigureIntent = intent;
    }

    public void setOnEstablishListener(OnEstablishListener listener) {
        mOnEstablishListener = listener;
    }

    public void stop() {
        isRunning.set(false);
        okHttpClientWrapper.stop();
        mOnEstablishListener = null;
    }

    @Override
    public void run() {
        try {
            Log.i(getTag(), "Starting");

            // todo: Add ConnectivityManager for check Internet
            for (int attempt = 0; attempt < 10; ++attempt) {
                // Reset the counter if we were connected.
                if (isRunning.get() == false) {
                    break;
                }
                if (startConnection()) {
                    attempt = 0;
                }
                // Sleep for a while. This also checks if we got interrupted.
                Thread.sleep(3000);
            }
            Log.i(getTag(), "Giving up");
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            Log.e(getTag(), "Connection failed, exiting", e);
        }
    }

    private boolean startConnection()
            throws IOException, InterruptedException, IllegalArgumentException {
        ParcelFileDescriptor vpnInterface = null;
        boolean connected = false;
        // Create a DatagramChannel as the VPN tunnel.
        try {
            String token = okHttpClientWrapper.getAuthToken();
            if (token == null) {
                sendMsgToUI(CONNECTED_FAILED, "Auth error!");
            } else {
                sendMsgToUI(CONNECTING, "");
            }

            //String dnsServer = okHttpClientWrapper.getDNSServer(serverName, serverPort);
            VpnService.Builder builder = service.new Builder();
            builder.addAddress("10.10.0.1", 32);
            builder.addRoute("172.20.0.1", 32);
            builder.addDnsServer("172.20.0.1"); // FIXME!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                builder.excludeRoute(new IpPrefix(InetAddress.getByName(serverHost), 32));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("10.10.0.0"), 16));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("172.16.0.0"), 12));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("192.168.0.0"), 16));
            } else {
                builder.addRoute(InetAddress.getByName(serverHost), 32);
            }
            builder.addRoute("0.0.0.0", 0);
            builder.setSession(serverHost).setConfigureIntent(mConfigureIntent);

            synchronized (service) {
                vpnInterface = builder.establish();
                if (vpnInterface == null) {
                    sendMsgToUI(CONNECTED_FAILED, "");
                } else {
                    sendMsgToUI(CONNECTED_SUCCESS, "");
                }
                if (mOnEstablishListener != null) {
                    mOnEstablishListener.onEstablish(vpnInterface);
                }
            }
            Log.i(getTag(), "New interface: " + vpnInterface);

            // Now we are connected. Set the flag.
            connected = true;


            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleWithFixedDelay(() -> {
                // Get download and upload speeds
                String downloadSpeed = downloadRate.getFormatString();
                String uploadSpeed = uploadRate.getFormatString();
                sendMsgToUI(SPEED_DOWNLOAD, String.valueOf(downloadSpeed));
                sendMsgToUI(SPEED_UPLOAD, String.valueOf(uploadSpeed));
            }, 1, 1, TimeUnit.SECONDS); // Start after 1 second, repeat every 1 second


            // Packets received need to be written to this output stream.
            FileOutputStream outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());
//            okHttpClientWrapper.startWebSocket(new CustomWebSocketListener(outputStream));

            WebSocketMessageCallback callback = new WebSocketMessageCallback() {
                @Override
                public void onMessageReceived(byte[] data) {
                    try {
                        downloadRate.update(data.length);
                        outputStream.write(data);
                    } catch (Exception e) {
                        Log.w(getTag(), "onMessageReceived: " + new String(data));
                    }
                }
            };
            okHttpClientWrapper.startWebSocket(new CustomWebSocketListener(callback));

            // Packets to be sent are queued in this input stream.
            FileInputStream inputStream = new FileInputStream(vpnInterface.getFileDescriptor());
            ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
            while (!Thread.interrupted()) {
//                Log.i(getTag(), "Thread: " + Thread.currentThread().getId());
                try {
                    int length = inputStream.read(buffer.array());
                    if (length > 0) {
                        uploadRate.update(length); // speed
                        okHttpClientWrapper.send(buffer, length);
                    }
                } catch (Exception e) {
                    Log.e(getTag(), "Error reading data from VPN interface: " + e.getMessage());
                }
            }
            sendMsgToUI(DISCONNECTED, "");
        } catch (PVNClientException e) {
            Log.e(getTag(), "Cannot use socket", e);
        } finally {
            if (vpnInterface != null) {
                try {
                    vpnInterface.close();
                    okHttpClientWrapper.stopWebSocket();
                } catch (IOException e) {
                    Log.e(getTag(), "Unable to close interface", e);
                }
            }
        }
        return connected;
    }

    private void sendMsgToUI(IntentMessageType type, String msg) {
        Intent intent = new Intent(MSG_INTENT_FILTER);
        intent.putExtra(MSG_TYPE, type.name());
        intent.putExtra(MSG_PAYLOAD, msg);
        LocalBroadcastManager.getInstance(service).sendBroadcast(intent);
    }

    private String getTag() {
        return this.getClass().getCanonicalName() + "[" + connectionId + "]";
    }

}
