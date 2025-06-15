package org.fptn.vpn.services.websocket;

import android.util.Log;

import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.services.websocket.callback.OnFailureCallback;
import org.fptn.vpn.services.websocket.callback.OnMessageReceivedCallback;
import org.fptn.vpn.services.websocket.callback.OnOpenCallback;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;

public class WebSocketClientWrapper {
    private static final String DNS_URL = "/api/v1/dns";
    private static final String LOGIN_URL = "/api/v1/login";

    private final FptnServerDto fptnServerDto;
    private final String tunAddress;
    private final String sniHostName;
    private final OnOpenCallback onOpenCallback;
    private final OnMessageReceivedCallback onMessageReceivedCallback;
    private final OnFailureCallback onFailureCallback;
    private final NativeHttpsClientImpl nativeHttpsClient;

    private NativeWebSocketClientImpl nativeWebSocketClient;

    @Getter
    private boolean shutdown = false;

    public WebSocketClientWrapper(FptnServerDto fptnServerDto,
                                  String tunAddress,
                                  String sniHostName,
                                  OnOpenCallback onOpenCallback,
                                  OnMessageReceivedCallback onMessageReceivedCallback,
                                  OnFailureCallback onFailureCallback) {
        this.fptnServerDto = fptnServerDto;
        this.tunAddress = tunAddress;
        this.sniHostName = sniHostName;
        this.onOpenCallback = onOpenCallback;
        this.onMessageReceivedCallback = onMessageReceivedCallback;
        this.onFailureCallback = onFailureCallback;
        this.nativeHttpsClient = new NativeHttpsClientImpl(fptnServerDto.host, fptnServerDto.port, this.sniHostName, fptnServerDto.md5ServerFingerprint);
    }

    public synchronized void startWebSocket() throws PVNClientException, WebSocketAlreadyShutdownException {
        if (isShutdown()) {
            throw new WebSocketAlreadyShutdownException();
        }
        stopWebSocket();

        String accessToken = getAccessToken(); // maybe move to constructor if it not changed between connections?
        nativeWebSocketClient = new NativeWebSocketClientImpl(
                fptnServerDto.host,
                fptnServerDto.port,
                tunAddress,
                sniHostName,
                accessToken,
                fptnServerDto.md5ServerFingerprint,
                onOpenCallback,
                onMessageReceivedCallback,
                onFailureCallback
        );
        Log.d(getTag(), "startWebSocket() nativeWebSocketClient.start() Thread.id: " + Thread.currentThread().getId());
        nativeWebSocketClient.start();
    }

    public synchronized void stopWebSocket() {
        Log.d(getTag(), "stopWebSocket()");
        if (nativeWebSocketClient != null) {
            if (nativeWebSocketClient.isStarted()) {
                Log.d(getTag(), "stopWebSocket() nativeWebSocketClient.stop() Thread.id: " + Thread.currentThread().getId());
                nativeWebSocketClient.stop();
            }
            /// if not explicit call, CG later call finilaze and release and Fatal signal 6 (SIGABRT), code -1 (SI_QUEUE) in tid 17404 (FinalizerDaemon), pid 17397 (org.fptn.vpn)
            //Log.d(getTag(), "stopWebSocket() nativeWebSocketClient.release() Thread.id: " + Thread.currentThread().getId());
            //nativeWebSocketClient.release();
            nativeWebSocketClient = null;
        }
    }

    public void send(byte[] bytes) {
        if (nativeWebSocketClient != null && nativeWebSocketClient.isStarted()) {
            nativeWebSocketClient.send(bytes);
        } else {
            throw new RuntimeException("nativeWebSocketClient is null or not started");
        }
    }

    public synchronized void shutdown() {
        stopWebSocket();
        shutdown = true;
    }

    private String getAccessToken() throws PVNClientException {
        String request = String.format("{\"username\": \"%s\", \"password\": \"%s\"}",
                fptnServerDto.username,
                fptnServerDto.password);

        NativeResponse response = nativeHttpsClient.Post(LOGIN_URL, request, 5);
        if (response != null) {
            if (response.code == 200) {
                try {
                    JSONObject jsonResponse = new JSONObject(response.body);
                    String accessToken = jsonResponse.getString("access_token");
                    Log.i(getTag(), "Getting accessToken successful.");
                    return accessToken;
                } catch (JSONException e) {
                    Log.e(getTag(), "Some error occurs on parsing accessToken response: " + e);
                    throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
                }
            }
        }
        throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
    }

    public String getDnsServerIPv4() throws PVNClientException {
        NativeResponse response = nativeHttpsClient.Get(DNS_URL, 5);
        if (response != null) {
            if (response.code == 200) {
                try {
                    JSONObject jsonResponse = new JSONObject(response.body);
                    String dnsServer = jsonResponse.getString("dns");
                    Log.i(getTag(), "DNS " + dnsServer + " retrieval successful.");
                    return dnsServer;
                } catch (JSONException e) {
                    Log.e(getTag(), "Some error occurs on receiving DNS response: " + e);
                    throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
                }
            }
        }
        throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }


}
