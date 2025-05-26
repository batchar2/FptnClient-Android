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
    private final OkHttpClient client;
    private final FptnServerDto fptnServerDto;
    private String token;

    private WebSocket webSocket;

    @Getter
    private boolean shutdown = false;

    public OkHttpWebSocketClientImpl(FptnServerDto fptnServerDto, String sniHostName) throws PVNClientException {
        this.fptnServerDto = fptnServerDto;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        }};

        // Install the all-trusting trust manager
        final SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(getTag(), "SSLContext init failed", e);
            throw new PVNClientException(ErrorCode.SSL_CONTEXT_INIT_FAILED.getValue());
        }

        // Create an SSL socket factory with our all-trusting manager
        final ChromeCiphers chromeCipers = new ChromeCiphers(sslContext);
        final String[] availableCiphers = chromeCipers.getAvailableCiphers();

        final MySSLSocketFactory sslSocketFactory = new MySSLSocketFactory(sslContext.getSocketFactory(), availableCiphers, sniHostName);
        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier((hostname, session) -> true);

        this.client = builder.build();
    }

    @Override
    public String getDnsServerIPv4() throws PVNClientException {
        NativeHttpsClientImpl client = new NativeHttpsClientImpl(fptnServerDto.host, fptnServerDto.port, "SNI", "TLS_FINGERPRINT");
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
            token = getAccessToken();
        }
        Request request = new Request.Builder().url(getUrlFromPattern(WEBSOCKET_URL)).addHeader("Authorization", "Bearer " + token)
                // The server needs to know the client's virtual interface for simplicity.
                .addHeader("ClientIP", "10.10.0.1").build();
        OkHttpWebSocketListener webSocketListener = new OkHttpWebSocketListener(onOpenCallback, onMessageReceivedCallback, onFailureCallback);
        webSocket = client.newWebSocket(request, webSocketListener);
    }

    @Override
    public void stopWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "stopWebSocket");
            webSocket = null;
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
        final int maxPayloadSize = 1450;
        if (webSocket != null && !isIPv6(data)) { // block IPv6
            ByteString payload = ByteString.copyFrom(data);

            // padding to random data
            ByteString padding = ByteString.EMPTY;
            if (payload.size() < maxPayloadSize) {
                final int paddingSize = maxPayloadSize - payload.size();
                padding = generateRandomBytes(paddingSize);
            }
            Protocol.IPPacket packet = Protocol.IPPacket.newBuilder()
                    .setPayload(payload)
                    .setPaddingData(padding)
                    .build();
            Protocol.Message msg = Protocol.Message.newBuilder()
                    .setProtocolVersion(1)
                    .setMsgType(Protocol.MessageType.MSG_IP_PACKET)
                    .setPacket(packet)
                    .build();
            webSocket.send(okio.ByteString.of(msg.toByteArray()));
        }
    }

    @NonNull
    private String getUrlFromPattern(String urlPattern) {
        return String.format(urlPattern, fptnServerDto.host, fptnServerDto.port);
    }

    private boolean isValid(String token) {
        //todo: Add token validation
        return token != null;
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X ", b));
        }
        return hexString.toString();
    }

    private boolean isIPv6(byte[] data) {
        return data.length > 0 && data[0] == 0x60;
    }

    private ByteString generateRandomBytes(int size) {
        byte[] randomBytes = new byte[size];
        SecureRandom secureRandom = new SecureRandom(); // Use SecureRandom for cryptographic safety
        secureRandom.nextBytes(randomBytes);
        return ByteString.copyFrom(randomBytes);
    }

}
