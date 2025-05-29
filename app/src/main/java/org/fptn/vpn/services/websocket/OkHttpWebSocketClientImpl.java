package org.fptn.vpn.services.websocket;

import android.util.Log;

import androidx.annotation.NonNull;

import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.services.websocket.callback.OnFailureCallback;
import org.fptn.vpn.services.websocket.callback.OnMessageReceivedCallback;
import org.fptn.vpn.services.websocket.callback.OnOpenCallback;
import org.fptn.vpn.utils.ChromeCiphers;
import org.fptn.vpn.utils.MySSLSocketFactory;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import com.google.protobuf.ByteString;

import org.fptn.protocol.Protocol;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.Getter;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;

public class OkHttpWebSocketClientImpl implements WebSocketClient {
    public static final MediaType JSON = MediaType.get("application/json");
    public static final String DNS_URL= "/api/v1/dns";
    public static final String LOGIN_URL = "/api/v1/login";
    public static final String WEBSOCKET_URL = "wss://%s:%d/fptn";
//    private final OkHttpClient client;


    private final FptnServerDto fptnServerDto;
    private String token;
    private String sniHostName;

    //private WebSocket webSocket;

    private NativeWebSocketClientImpl webSocket = null;

    @Getter
    private boolean shutdown = false;

    public OkHttpWebSocketClientImpl(FptnServerDto fptnServerDto, String sniHostName) throws PVNClientException {
        this.fptnServerDto = fptnServerDto;
        this.sniHostName = sniHostName;
    }

    @Override
    public String getDnsServerIPv4() throws PVNClientException {
        NativeHttpsClientImpl client = new NativeHttpsClientImpl(fptnServerDto.host, fptnServerDto.port, this.sniHostName, "TLS_FINGERPRINT");
        NativeResponse response = client.Get(DNS_URL, 5);
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

    @Override
    public void startWebSocket(OnOpenCallback onOpenCallback, OnMessageReceivedCallback onMessageReceivedCallback, OnFailureCallback onFailureCallback) throws PVNClientException, WebSocketAlreadyShutdownException {
        if (isShutdown()) {
            throw new WebSocketAlreadyShutdownException();
        }
        if (!isValid(token)) {
            this.token = getAccessToken();
        }

        this.webSocket = new NativeWebSocketClientImpl(this.fptnServerDto, this.sniHostName,  this.token);
        this.webSocket.startWebSocket(onOpenCallback, onMessageReceivedCallback, onFailureCallback);
    }

    @Override
    public void stopWebSocket() {
        if (this.webSocket != null) {
            this.webSocket.stopWebSocket();
            this.webSocket = null;
            this.token = null;
        }
    }

    @Override
    public void shutdown() {
        shutdown = true;
        stopWebSocket();
    }

    private String getAccessToken() throws PVNClientException {
        String request = String.format("{\"username\": \"%s\", \"password\": \"%s\"}",
                fptnServerDto.username,
                fptnServerDto.password);
        NativeHttpsClientImpl client = new NativeHttpsClientImpl(fptnServerDto.host, fptnServerDto.port, "SNI", "TLS_FINGERPRINT");
        NativeResponse response = client.Post(LOGIN_URL, request, 5);
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

    @Override
    public void send(byte[] data) {
        final int maxPayloadSize = 1500;
        if (webSocket != null && !isIPv6(data)) { // block IPv6
            ByteString payload = ByteString.copyFrom(data);
            this.webSocket.send(payload.toByteArray());
        }
    }

    private boolean isValid(String token) {
        //todo: Add token validation
        return token != null;
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }

    private boolean isIPv6(byte[] data) {
        return data.length > 0 && data[0] == 0x60;
    }
}
