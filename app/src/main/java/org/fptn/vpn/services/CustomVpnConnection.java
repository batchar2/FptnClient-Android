package org.fptn.vpn.services;

import android.app.PendingIntent;
import android.net.IpPrefix;
import android.net.VpnService;
import android.os.Build;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.enums.HandlerMessageTypes;
import org.fptn.vpn.services.websocket.WebSocketClient;
import org.fptn.vpn.services.websocket.OkHttpWebSocketClientImpl;
import org.fptn.vpn.services.websocket.WebSocketAlreadyShutdownException;
import org.fptn.vpn.utils.DataRateCalculator;
import org.fptn.vpn.utils.IPUtils;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
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
    /**
     * Maximum packet size is constrained by the MTU
     */
    private static final int MAX_PACKET_SIZE = 1500;
    private static final int MAX_RECONNECT_COUNT = 10;

    private final CustomVpnService service;
    @Getter
    private final int connectionId;
    private final FptnServerDto fptnServerDto;
    private final WebSocketClient webSocketClient;

    @Setter
    private PendingIntent configureVpnIntent;

    private ParcelFileDescriptor vpnInterface;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final DataRateCalculator downloadRate = new DataRateCalculator(1000);
    private final DataRateCalculator uploadRate = new DataRateCalculator(1000);

    private final Thread currentThread = this;
    private final AtomicInteger reconnectCount = new AtomicInteger(0);
    @Getter
    private volatile ConnectionState currentConnectionState = ConnectionState.DISCONNECTED;
    @Getter
    private Instant connectionTime;
    private FileOutputStream outputStream;


    public CustomVpnConnection(final CustomVpnService service, int connectionId, final FptnServerDto fptnServerDto, String sniHostName) throws PVNClientException {
        this.service = service;
        this.connectionId = connectionId;
        this.fptnServerDto = fptnServerDto;
        this.webSocketClient = new OkHttpWebSocketClientImpl(fptnServerDto, sniHostName);
    }

    @Override
    public void run() {
        vpnInterface = null;
        try {
            sendConnectionStateToService(ConnectionState.CONNECTING);

            VpnService.Builder builder = service.new Builder();
            builder.addAddress("10.10.0.1", 32);
            builder.addRoute("172.20.0.1", 32);
            builder.setMtu(MAX_PACKET_SIZE);

            final String dnsServer = webSocketClient.getDnsServerIPv4();
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

            builder.setSession(fptnServerDto.name).setConfigureIntent(configureVpnIntent);

            synchronized (service) {
                vpnInterface = builder.establish();
            }
            if (vpnInterface == null) {
                throw new PVNClientException(ErrorCode.VPN_INTERFACE_ERROR.getValue());
            }
            Log.i(getTag(), "New interface: " + vpnInterface);

            try {
                scheduler.scheduleWithFixedDelay(() -> {
                    // Get download and upload speeds
                    String downloadSpeed = downloadRate.getFormatString();
                    String uploadSpeed = uploadRate.getFormatString();
                    sendSpeedInfoToService(downloadSpeed, uploadSpeed);
                }, 1, 1, TimeUnit.SECONDS); // Start after 1 second, repeat every 1 second
            } catch (RejectedExecutionException e) {
                Log.w(getTag(), "update speed task rejected by scheduler", e);
            }


            // Packets received need to be written to this output stream.
            outputStream = new FileOutputStream(vpnInterface.getFileDescriptor());
            webSocketClient.startWebSocket(this::onConnectionOpen, this::onMessageReceived, this::onConnectionFailure);
            connectionTime = Instant.now();

            // Packets to be sent are queued in this input stream.
            try (FileInputStream inputStream = new FileInputStream(vpnInterface.getFileDescriptor())) {
                byte[] byteBuffer = new byte[MAX_PACKET_SIZE];
                while (!currentThread.isInterrupted()) {
                    try {
                        int length = inputStream.read(byteBuffer);
                        if (length > 0) {
                            uploadRate.update(length);
                            webSocketClient.send(Arrays.copyOf(byteBuffer, length));
                        }
                    } catch (Exception e) {
                        Log.d(getTag(), "Error reading data from VPN interface: " + e.getMessage());
                    }
                }
            }
        } catch (PVNClientException | IOException e) {
            sendErrorMessageToService(e.getMessage());
        } catch (WebSocketAlreadyShutdownException e) {
            Log.w(getTag(), "The websocket already shutdown", e);
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        if (!currentThread.isInterrupted()) {
            currentThread.interrupt();
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException e) {
                Log.e(getTag(), "Unable to close interface", e);
            }
        }
        webSocketClient.shutdown();
        scheduler.shutdown();

        sendConnectionStateToService(ConnectionState.DISCONNECTED);
    }

    private void onConnectionOpen() {
        if (!currentThread.isInterrupted()) {
            sendConnectionStateToService(ConnectionState.CONNECTED);
            reconnectCount.set(0);
        }
    }

    private void onMessageReceived(byte[] data) {
        try {
            if (outputStream != null) {
                downloadRate.update(data.length);
                outputStream.write(data);
            }
        } catch (Exception e) {
            Log.w(getTag(), "Ошибка записи в TUN: " + new String(data));
        }
    }

    private void onConnectionFailure() {
        if (!currentThread.isInterrupted()) {
            int currentCount = reconnectCount.incrementAndGet();
            Log.i(getTag(), "Reconnect WebSocket... currentCount: " + currentCount);
            if (isTunInterfaceValid(vpnInterface) && currentCount < MAX_RECONNECT_COUNT) {
                try {
                    sendConnectionStateToService(ConnectionState.RECONNECTING);
                    webSocketClient.stopWebSocket();
                    Thread.sleep(2000);
                    webSocketClient.startWebSocket(this::onConnectionOpen, this::onMessageReceived, this::onConnectionFailure);
                } catch (PVNClientException e) {
                    sendErrorMessageToService(e.getMessage());
                } catch (InterruptedException e) {
                    Log.w(getTag(), "The thread was interrupted", e);
                } catch (WebSocketAlreadyShutdownException e) {
                    Log.w(getTag(), "The websocket already shutdown", e);
                }
            } else {
                currentThread.interrupt();
                sendErrorMessageToService(ErrorCode.RECONNECTING_FAILED.getValue());
            }
        }
    }

    private void sendErrorMessageToService(String msg) {
        service.getHandler().sendMessage(Message.obtain(service.getHandler(), HandlerMessageTypes.ERROR.getValue(), connectionId, 0, msg));
    }

    private void sendSpeedInfoToService(String downloadSpeed, String uploadSpeed) {
        service.getHandler().sendMessage(
                Message.obtain(service.getHandler(), HandlerMessageTypes.SPEED_INFO.getValue(), connectionId, 0,
                        new Triple<>(downloadSpeed, uploadSpeed, fptnServerDto.getServerInfo()))
        );
    }

    private void sendConnectionStateToService(ConnectionState connectionState) {
        currentConnectionState = connectionState;
        service.getHandler().sendMessage(
                Message.obtain(service.getHandler(), HandlerMessageTypes.CONNECTION_STATE.getValue(), connectionId, reconnectCount.get(),
                        new Triple<>(connectionState, Instant.now(), fptnServerDto.getServerInfo()))
        );
    }

    private boolean isTunInterfaceValid(ParcelFileDescriptor vpnInterface) {
        return vpnInterface.getFileDescriptor() != null;
    }

    private String getTag() {
        return this.getClass().getCanonicalName() + "[" + connectionId + "]";
    }

}
