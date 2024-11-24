package com.filantrop.pvnclient;

import android.app.PendingIntent;
import android.net.IpPrefix;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;

import com.filantrop.pvnclient.exception.PVNClientException;
import com.filantrop.pvnclient.websocket.CustomWebSocketListener;
import com.filantrop.pvnclient.websocket.OkHttpClientWrapper;

import org.fptn.protocol.Protocol;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

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
    private static final int MAX_PACKET_SIZE = 65536;

    private final VpnService service;
    private final int connectionId;

    private final String serverName;
    private final int serverPort;

    private final OkHttpClientWrapper okHttpClientWrapper;

    private PendingIntent mConfigureIntent;
    private OnEstablishListener mOnEstablishListener;

    public CustomVpnConnection(final VpnService service, final int connectionId,
                               final String serverName, final int serverPort,
                               final String username, final String password) {
        this.service = service;
        this.connectionId = connectionId;

        this.serverName = serverName;
        this.serverPort = serverPort;

        this.okHttpClientWrapper = new OkHttpClientWrapper(username, password);
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

    @Override
    public void run() {
        try {
            Log.i(getTag(), "Starting");

            // todo: Add ConnectivityManager for check Internet
            for (int attempt = 0; attempt < 10; ++attempt) {
                // Reset the counter if we were connected.
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
            //String dnsServer = okHttpClientWrapper.getDNSServer(serverName, serverPort);

            // VPNService используется только, чтобы собрать весь трафик с устройства,
            // весь обмен идет по webSocket, поэтому значения фиксированные

            VpnService.Builder builder = service.new Builder();
            builder.addAddress("10.10.0.1", 32);
            builder.addRoute("172.20.0.1", 32);
            builder.excludeRoute(new IpPrefix(InetAddress.getByName(serverName), 32));
            builder.addRoute("0.0.0.0", 0);
            // builder.setMtu();
            //add fakeIP
   /*          builder.addAddress("10.10.0.1", 32);
            builder.addDnsServer(dnsServer);

            builder.addRoute(dnsServer, 32);
            builder.excludeRoute(new IpPrefix(InetAddress.getByName(serverName), 32));
            builder.addRoute("0.0.0.0", 0);
            //builder.addSearchDomain();*/


            // Create a new interface using the builder and save the parameters.
            builder.setSession(serverName).setConfigureIntent(mConfigureIntent);

            synchronized (service) {
                vpnInterface = builder.establish();
                if (mOnEstablishListener != null) {
                    mOnEstablishListener.onEstablish(vpnInterface);
                }
            }
            Log.i(getTag(), "New interface: " + vpnInterface);

            // Now we are connected. Set the flag.
            connected = true;

            // Packets received need to be written to this output stream.
            FileOutputStream outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());
            okHttpClientWrapper.startWebSocket(serverName, serverPort, new CustomWebSocketListener(outputStream));

            // Packets to be sent are queued in this input stream.
            FileInputStream inputStream = new FileInputStream(vpnInterface.getFileDescriptor());
            ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
            while (!Thread.interrupted()) {
                Log.i(getTag(), "Thread: " + Thread.currentThread().getId());
                try {
                    int length = inputStream.read(buffer.array());
                    if (length > 0) {
                        okHttpClientWrapper.send(buffer, length);
                    }
                } catch (Exception e) {
                    Log.e(getTag(), "Error reading data from VPN interface: " + e.getMessage());
                }
            }
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

    private String getTag() {
        return this.getClass().getCanonicalName() + "[" + connectionId + "]";
    }
}
