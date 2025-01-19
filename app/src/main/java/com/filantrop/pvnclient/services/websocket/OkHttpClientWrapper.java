package com.filantrop.pvnclient.services.websocket;

import android.util.Log;

import com.filantrop.pvnclient.utils.ChromeCiphers;
import com.filantrop.pvnclient.utils.MySSLSocketFactory;
import com.filantrop.pvnclient.vpnclient.exception.ErrorCode;
import com.filantrop.pvnclient.vpnclient.exception.PVNClientException;
import com.google.protobuf.ByteString;

import org.fptn.protocol.Protocol;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;


public class OkHttpClientWrapper {
    public static final MediaType JSON = MediaType.get("application/json");
    public static final String DNS_URL_PATTERN = "https://%s:%d/api/v1/dns";
    public static final String LOGIN_URL_PATTERN = "https://%s:%d/api/v1/login";
    public static final String WEBSOCKET_URL = "wss://%s:%d/fptn";

    private final OkHttpClient client;

    private final String username;
    private final String password;

    private final String host;
    private final int port;

    private String token;

    private WebSocket webSocket;

    public OkHttpClientWrapper(String username, String password, String host, int port) throws PVNClientException {
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
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
                }
        };

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

        final SSLSocketFactory sslSocketFactory = new MySSLSocketFactory(sslContext.getSocketFactory(), availableCiphers);
        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier((hostname, session) -> true);

        this.client = builder.build();
    }

    public String getAuthToken() throws PVNClientException {
        Map<String, String> map = new HashMap<>();
        map.put("username", username);
        map.put("password", password);
        JSONObject json = new JSONObject(map);

        final String url = String.format(Locale.getDefault(), LOGIN_URL_PATTERN, host, port);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json.toString(), JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200 && response.body() != null) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                if (jsonResponse.has("access_token")) {
                    final String token = jsonResponse.getString("access_token");
                    Log.i(getTag(), "Login successful.");
                    return token;
                } else {
                    Log.e(getTag(), "Error: Access token not found in the response.");
                }
            } else {
                Log.e(getTag(), "Error response: " + response);
            }
        } catch (JSONException | IOException e) {
            Log.e(getTag(), "Login failed", e);
        }
        throw new PVNClientException(ErrorCode.ACCESS_TOKEN_ERROR.getValue());
    }

    public String getDnsServerIPv4() throws PVNClientException {
        final String url = String.format(Locale.getDefault(), DNS_URL_PATTERN, host, port);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200 && response.body() != null) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                if (jsonResponse.has("dns")) {
                    final String dnsServer = jsonResponse.getString("dns");
                    Log.i(getTag(), "DNS " + dnsServer + " retrieval successful.");
                    return dnsServer;
                } else {
                    Log.e(getTag(), "Error: DNS not found in the response.");
                }
            } else {
                Log.e(getTag(), "Error response: " + response);
            }
        } catch (JSONException | IOException e) {
            Log.e(getTag(), "Receiving DNS failed", e);
        }
        throw new PVNClientException(ErrorCode.DNS_SERVER_ERROR.getValue());
    }

    public void startWebSocket(CustomWebSocketListener webSocketListener) throws PVNClientException {
        if (!isValid(token)) {
            token = getAuthToken();
        }
        Request request = new Request.Builder()
                .url(String.format(Locale.getDefault(), WEBSOCKET_URL, host, port))
                .addHeader("Authorization", "Bearer " + token)
                // The server needs to know the client's virtual interface for simplicity.
                .addHeader("ClientIP", "10.10.0.1")
                .build();
        webSocket = client.newWebSocket(request, webSocketListener);
    }

    public void stopWebSocket() {
        if (webSocket != null) {
            webSocket.close(1000, "stopWebSocket");
            webSocket = null;
        }
    }

    private boolean isValid(String token) {
        //todo: Add token validation
        return token != null;
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }

    public void send(ByteBuffer buffer, int length) {
        final int maxPayloadSize = 1450;
        if (webSocket != null) {
            byte[] copyBuffer = new byte[length];
            System.arraycopy(buffer.array(), 0, copyBuffer, 0, length);
            ByteString payload = ByteString.copyFrom(copyBuffer);

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

    public static ByteString generateRandomBytes(int size) {
        byte[] randomBytes = new byte[size];
        SecureRandom secureRandom = new SecureRandom(); // Use SecureRandom for cryptographic safety
        secureRandom.nextBytes(randomBytes);
        return ByteString.copyFrom(randomBytes);
    }
}
