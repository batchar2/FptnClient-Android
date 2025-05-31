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

public class NativeWebSocketClientImpl {
    static {
        System.loadLibrary("fptn_native_lib");
    }

    private static final String DNS_URL = "/api/v1/dns";
    private static final String LOGIN_URL = "/api/v1/login";

    private final FptnServerDto fptnServerDto;
    private final OnOpenCallback onOpenCallback;
    private final OnMessageReceivedCallback onMessageReceivedCallback;
    private final OnFailureCallback onFailureCallback;
    private final NativeHttpsClientImpl nativeHttpsClient;

    private long nativeHandle = 0L;

    @Getter
    private boolean shutdown = false;

    public NativeWebSocketClientImpl(FptnServerDto fptnServerDto, String sniHostName, OnOpenCallback onOpenCallback, OnMessageReceivedCallback onMessageReceivedCallback, OnFailureCallback onFailureCallback) throws PVNClientException {
        this.fptnServerDto = fptnServerDto;
        this.onOpenCallback = onOpenCallback;
        this.onMessageReceivedCallback = onMessageReceivedCallback;
        this.onFailureCallback = onFailureCallback;

        this.nativeHttpsClient = new NativeHttpsClientImpl(fptnServerDto.host, fptnServerDto.port, sniHostName, fptnServerDto.md5ServerFingerprint);
        this.nativeHandle = nativeCreate(
                fptnServerDto.host,
                fptnServerDto.port,
                //todo: move all magic ip to constants class with description
                "10.10.0.1",
                sniHostName,
                getAccessToken(),
                fptnServerDto.md5ServerFingerprint
        );
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
                    throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR.getValue());
                }
            }
        }
        throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR.getValue());
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
                    throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR.getValue());
                }
            }
        }
        throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR.getValue());
    }

    public void startWebSocket() throws PVNClientException, WebSocketAlreadyShutdownException {
        if (isShutdown()) {
            throw new WebSocketAlreadyShutdownException();
        }

        if (!nativeIsStarted(nativeHandle)) {
            nativeRun(nativeHandle);
        }
    }

    public void stopWebSocket() {
        if (nativeIsStarted(nativeHandle)) {
            nativeStop(nativeHandle);
        }
    }

    public void shutdown() {
        shutdown = true;
    }

    public void send(byte[] data) {
        if (nativeHandle != 0L && nativeIsStarted(nativeHandle)) {
            nativeSend(nativeHandle, data);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(getTag(), "NativeWebsocketWrapper.finalize()");
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0L;
        }
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }

    public void onOpenImpl() {
        Log.d(getTag(), "NativeWebsocketWrapper.onOpenImpl():start()");
        if (this.onOpenCallback != null) {
            this.onOpenCallback.onOpen();
        }
        Log.d(getTag(), "NativeWebsocketWrapper.onOpenImpl():end()");
    }

    public void onCloseImpl() {
        Log.d(getTag(), "NativeWebsocketWrapper.onCloseImpl():start()");
        if (this.onFailureCallback != null) {
            this.onFailureCallback.onFailure();
        }
        Log.d(getTag(), "NativeWebsocketWrapper.onCloseImpl():end()");
    }

    public void onMessageImpl(byte[] msg) {
        if (this.onMessageReceivedCallback != null) {
            this.onMessageReceivedCallback.onMessageReceived(msg);
        }
    }

    private native long nativeCreate(String server_ip,
                                     int server_port,
                                     String tun_ipv4,
                                     String sni,
                                     String access_token,
                                     String expected_md5_fingerprint);

    private native void nativeDestroy(long nativeHandle);

    private native boolean nativeRun(long nativeHandle);

    private native boolean nativeStop(long nativeHandle);

    private native boolean nativeSend(long nativeHandle, byte[] data);

    private native boolean nativeIsStarted(long nativeHandle);
}
