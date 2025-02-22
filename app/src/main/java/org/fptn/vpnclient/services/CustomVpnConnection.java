package org.fptn.vpnclient.services;

import android.app.PendingIntent;
import android.net.IpPrefix;
import android.net.VpnService;
import android.os.Build;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.fptn.vpnclient.database.model.FptnServerDto;
import org.fptn.vpnclient.enums.ConnectionState;
import org.fptn.vpnclient.enums.HandlerMessageTypes;
import org.fptn.vpnclient.services.websocket.CustomWebSocketListener;
import org.fptn.vpnclient.services.websocket.OkHttpClientWrapper;
import org.fptn.vpnclient.utils.DataRateCalculator;
import org.fptn.vpnclient.utils.IPUtils;
import org.fptn.vpnclient.vpnclient.exception.ErrorCode;
import org.fptn.vpnclient.vpnclient.exception.PVNClientException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import kotlin.Triple;
import lombok.Getter;
import lombok.Setter;

public class CustomVpnConnection extends Thread {

    public interface OnEstablishListener {
        void onEstablish(ParcelFileDescriptor tunInterface);
    }

    /**
     * Maximum packet size is constrained by the MTU
     */
    private static final int MAX_PACKET_SIZE = 65536;
    private static final int MAX_RECONNECT_COUNT = 10;

    private final CustomVpnService service;

    @Getter
    private final int connectionId;

    private final FptnServerDto fptnServerDto;

    private OkHttpClientWrapper okHttpClientWrapper;

    private PendingIntent mConfigureIntent;

    @Setter
    private OnEstablishListener onEstablishListener;

    @Getter
    private Instant connectionTime;

    private CustomWebSocketListener customWebSocketListener;

    private ParcelFileDescriptor vpnInterface;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final DataRateCalculator downloadRate = new DataRateCalculator(1000);
    private final DataRateCalculator uploadRate = new DataRateCalculator(1000);

    private Thread currentConnectionThread;

    private final AtomicInteger reconnectCount = new AtomicInteger(0);


    public CustomVpnConnection(final CustomVpnService service, int connectionId, final FptnServerDto fptnServerDto) {
        this.service = service;
        this.connectionId = connectionId;
        this.fptnServerDto = fptnServerDto;
        try {
            this.okHttpClientWrapper = new OkHttpClientWrapper(fptnServerDto);
        } catch (PVNClientException ex) {
            sendErrorMessageToUI(ex.getMessage());
        }
    }

    /**
     * Optionally, set an intent to configure the VPN. This is {@code null} by default.
     */
    public void setConfigureIntent(PendingIntent intent) {
        mConfigureIntent = intent;
    }

    @Override
    public void run() {
        vpnInterface = null;
        try {
            sendConnectionStateToUI(ConnectionState.CONNECTING);

            final String token = okHttpClientWrapper.getAuthToken();
            if (token == null) {
                throw new PVNClientException(ErrorCode.AUTHENTICATION_ERROR.getValue());
            }

            VpnService.Builder builder = service.new Builder();
            builder.addAddress("10.10.0.1", 32);
            builder.addRoute("172.20.0.1", 32);

            final String dnsServer = okHttpClientWrapper.getDnsServerIPv4();
            builder.addDnsServer(dnsServer);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                builder.excludeRoute(new IpPrefix(InetAddress.getByName(fptnServerDto.host), 32));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("10.10.0.0"), 16));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("172.16.0.0"), 12));
                builder.excludeRoute(new IpPrefix(InetAddress.getByName("192.168.0.0"), 16));
                builder.addRoute("0.0.0.0", 0);
            } else {
                IPAddress rootSubnet = new IPAddressString("0.0.0.0/0").getAddress();
                List<IPAddress> subnetsToExclude = Stream.of(
                                fptnServerDto.host + "/32",
                                "10.10.0.0/16",
                                "172.16.0.0/12",
                                "192.168.0.0/16")
                        .map(sub -> new IPAddressString(sub).getAddress()).collect(Collectors.toList());

                List<IPAddress> subnetsToInclude = new ArrayList<>();
                IPUtils.exclude(rootSubnet, subnetsToExclude, subnetsToInclude);
                for (IPAddress ipAddress : subnetsToInclude) {
                    String hostIp = ipAddress.getLower().toAddressString().getHostAddress().toString();
                    Integer networkPrefixLength = ipAddress.getLower().toAddressString().getNetworkPrefixLength();
                    Log.d(getTag(), "subnetsToInclude.ipAddress: " + hostIp + "/" + networkPrefixLength);
                    builder.addRoute(hostIp, networkPrefixLength != null ? networkPrefixLength : 32);
                }
            }

            builder.setSession(fptnServerDto.name).setConfigureIntent(mConfigureIntent);

            synchronized (service) {
                vpnInterface = builder.establish();
            }
            if (vpnInterface == null) {
                throw new PVNClientException(ErrorCode.VPN_INTERFACE_ERROR.getValue());
            } else {
                if (onEstablishListener != null) {
                    onEstablishListener.onEstablish(vpnInterface);
                }
            }
            Log.i(getTag(), "New interface: " + vpnInterface);

            scheduler.scheduleWithFixedDelay(() -> {
                // Get download and upload speeds
                String downloadSpeed = downloadRate.getFormatString();
                String uploadSpeed = uploadRate.getFormatString();
                sendSpeedInfoToUI(downloadSpeed, uploadSpeed);
            }, 1, 1, TimeUnit.SECONDS); // Start after 1 second, repeat every 1 second

            // Packets received need to be written to this output stream.
            FileOutputStream outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());
            customWebSocketListener = new CustomWebSocketListener(data -> onMessageReceived(data, outputStream), this::onConnectionClosed, this::onConnectionOpen);
            okHttpClientWrapper.startWebSocket(customWebSocketListener);
            connectionTime = Instant.now();

            // Packets to be sent are queued in this input stream.
            FileInputStream inputStream = new FileInputStream(vpnInterface.getFileDescriptor());
            ByteBuffer buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);

            currentConnectionThread = Thread.currentThread();
            while (!isInterrupted()) {
                try {
                    int length = inputStream.read(buffer.array());
                    if (length > 0) {
                        uploadRate.update(length);
                        okHttpClientWrapper.send(buffer, length);
                    }
                } catch (Exception e) {
                    Log.d(getTag(), "Error reading data from VPN interface: " + e.getMessage());
                }
            }
        } catch (PVNClientException | IOException e) {
            sendErrorMessageToUI(e.getMessage());
        } finally {
            sendConnectionStateToUI(ConnectionState.DISCONNECTED);
            if (vpnInterface != null) {
                try {
                    vpnInterface.close();
                } catch (IOException e) {
                    Log.e(getTag(), "Unable to close interface", e);
                }
            }
            okHttpClientWrapper.stopWebSocket();
            scheduler.shutdown();
        }
    }

    private void onConnectionOpen() {
        sendConnectionStateToUI(ConnectionState.CONNECTED);
        reconnectCount.set(0);
    }

    private void onMessageReceived(byte[] data, FileOutputStream outputStream) {
        try {
            downloadRate.update(data.length);
            outputStream.write(data);
        } catch (Exception e) {
            Log.w(getTag(), "Ошибка записи в TUN: " + new String(data));
        }
    }

    private void onConnectionClosed() {
        if (currentConnectionThread != null && !currentConnectionThread.isInterrupted()) {
            int currentCount = reconnectCount.incrementAndGet();
            Log.i(getTag(), "Reconnect WebSocket... currentCount: " + currentCount);
            if (isTunInterfaceValid(vpnInterface) && currentCount < MAX_RECONNECT_COUNT) {
                try {
                    sendConnectionStateToUI(ConnectionState.RECONNECTING);
                    okHttpClientWrapper.stopWebSocket();
                    Thread.sleep(2000);
                    okHttpClientWrapper.startWebSocket(customWebSocketListener);
                } catch (PVNClientException e) {
                    sendErrorMessageToUI(e.getMessage());
                } catch (InterruptedException e) {
                    Log.w(getTag(), "The thread was interrupted", e);
                }
            } else {
                //todo: change on check network connection with recreating vpnInterface
                if (!currentConnectionThread.isInterrupted()) {
                    currentConnectionThread.interrupt();
                }
                //todo: send notification to user that we can't connect maybe with reconnect intent button
            }
        }
    }

    private void sendErrorMessageToUI(String msg) {
        service.getHandler().sendMessage(Message.obtain(null, HandlerMessageTypes.ERROR.getValue(), 0, 0, msg));
    }

    private void sendSpeedInfoToUI(String downloadSpeed, String uploadSpeed) {
        service.getHandler().sendMessage(
                Message.obtain(null, HandlerMessageTypes.SPEED_INFO.getValue(), 0, 0,
                        new Triple<>(downloadSpeed, uploadSpeed, fptnServerDto.getServerInfo()))
        );
    }

    private void sendConnectionStateToUI(ConnectionState connectionState) {
        service.getHandler().sendMessage(
                Message.obtain(null, HandlerMessageTypes.CONNECTION_STATE.getValue(), 0, 0,
                        new Triple<>(connectionState, Instant.now(), fptnServerDto.getServerInfo()))
        );
    }

    private boolean isTunInterfaceValid(ParcelFileDescriptor vpnInterface) {
        //todo: add check change network condition
        return vpnInterface.getFileDescriptor() != null;
    }

    private String getTag() {
        return this.getClass().getCanonicalName() + "[" + connectionId + "]";
    }

}
