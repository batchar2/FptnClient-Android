package com.filantrop.pvnclient;

import android.app.PendingIntent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.filantrop.pvnclient.websocket.OkHttpClientWrapper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

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

            final SocketAddress serverAddress = new InetSocketAddress(serverName, serverPort);
            // todo: Add ConnectivityManager for check Internet
            for (int attempt = 0; attempt < 10; ++attempt) {
                // Reset the counter if we were connected.
                if (startConnection(serverAddress)) {
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

    private boolean startConnection(SocketAddress server)
            throws IOException, InterruptedException, IllegalArgumentException {
        ParcelFileDescriptor vpnInterface = null;
        boolean connected = false;
        // Create a DatagramChannel as the VPN tunnel.
        try (DatagramChannel tunnel = DatagramChannel.open()) {

            // Protect the tunnel before connecting to avoid loopback.
            if (!service.protect(tunnel.socket())) {
                throw new IllegalStateException("Cannot protect the tunnel");
            }

            // Connect to the server.
            tunnel.connect(server);

            // For simplicity, we use the same thread for both reading and
            // writing. Here we put the tunnel into non-blocking mode.
            tunnel.configureBlocking(false);

            String dnsServer = okHttpClientWrapper.getDNSServer(serverName, serverPort);
            /*okHttpClientWrapper.startWebSocket(serverName, serverPort, new WebSocketListener() {
                @Override
                public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
                    super.onMessage(webSocket, bytes);
                }
            });*/

            // VPNService используется только, чтобы собрать весь трафик с устройства,
            // весь обмен идет по webSocket, поэтому значения фиксированные
            VpnService.Builder builder = service.new Builder();

            //builder.setMtu();
            builder.addAddress("10.10.0.1", 32);
            //builder.addRoute();
            builder.addDnsServer(dnsServer);
            //builder.addSearchDomain();

            /*            builder.addAddress("10.10.0.1", 32);
            builder.addRoute("172.20.0.1", 32);
            builder.excludeRoute(new IpPrefix(InetAddress.getByName(vpnHost), 32));
            builder.addRoute("0.0.0.0", 0);*/

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

            // Packets to be sent are queued in this input stream.
            FileInputStream in = new FileInputStream(vpnInterface.getFileDescriptor());

            // Packets received need to be written to this output stream.
            FileOutputStream out = new FileOutputStream(vpnInterface.getFileDescriptor());

            // Allocate the buffer for a single packet.
            ByteBuffer packet = ByteBuffer.allocate(MAX_PACKET_SIZE);

            // We keep forwarding packets till something goes wrong.
            while (true) {
                // Read the outgoing packet from the input stream.
                int length = in.read(packet.array());
                if (length > 0) {
                    // Write the outgoing packet to the tunnel.
                    packet.limit(length);
                    tunnel.write(packet);
                    packet.clear();
                }

                // Read the incoming packet from the tunnel.
                length = tunnel.read(packet);
                if (length > 0) {
                    // Ignore control messages, which start with zero.
                    if (packet.get(0) != 0) {
                        // Write the incoming packet to the output stream.
                        out.write(packet.array(), 0, length);
                    }
                    packet.clear();
                }

            }
        } catch (SocketException e) {
            Log.e(getTag(), "Cannot use socket", e);
        } finally {
            if (vpnInterface != null) {
                try {
                    vpnInterface.close();
                } catch (IOException e) {
                    Log.e(getTag(), "Unable to close interface", e);
                }
            }
        }
        return connected;
    }

    private String getTag() {
        return CustomVpnConnection.class.getSimpleName() + "[" + connectionId + "]";
    }
}
