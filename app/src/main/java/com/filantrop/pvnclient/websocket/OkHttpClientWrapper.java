package com.filantrop.pvnclient.websocket;

import android.util.Log;

import com.filantrop.pvnclient.exception.PVNClientException;
import com.google.protobuf.ByteString;

import org.fptn.protocol.Protocol;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;

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
import okhttp3.WebSocketListener;

public class OkHttpClientWrapper {
    public static final MediaType JSON = MediaType.get("application/json");

    public static final String LOGIN_URL_PATTERN = "https://%s:%d/api/v1/login";
    public static final String DNS_URL_PATTERN = "https://%s:%d/api/v1/dns";
    public static final String WEBSOCKET_URL = "wss://%s:%d/fptn";

    private final OkHttpClient client;

    private final String username;
    private final String password;

    private String token;

    private WebSocket webSocket;

    public OkHttpClientWrapper(String username, String password) {
        this.username = username;
        this.password = password;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
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
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(getTag(), "Login failed", e);
            throw PVNClientException.fromException(e);
        }
        // Create an SSL socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier((hostname, session) -> true);

        this.client = builder.build();
    }

    private String getAuthToken(String host, int port) {
        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("password", password);

            String url = String.format(Locale.getDefault(), LOGIN_URL_PATTERN, host, port);
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(json.toString(), JSON))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 200 && response.body() != null) {
                    JSONObject jsonResponse = new JSONObject(response.body().string());
                    if (jsonResponse.has("access_token")) {
                        String token = jsonResponse.getString("access_token");
                        Log.i(getTag(), "Login successful.");
                        return token;
                    } else {
                        Log.e(getTag(), "Error: Access token not found in the response.");
                        throw PVNClientException.fromMessage("Error: Access token not found in the response.");
                    }
                } else {
                    Log.e(getTag(), "Error response: " + response);
                }
            }

        } catch (JSONException | IOException e) {
            Log.e(getTag(), "Login failed", e);
            throw PVNClientException.fromException(e);
        }
        return null;
    }

    public String getDNSServer(String host, int port) {
        try {
            String url = String.format(Locale.getDefault(), DNS_URL_PATTERN, host, port);
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 200 && response.body() != null) {
                    JSONObject jsonResponse = new JSONObject(response.body().string());
                    if (jsonResponse.has("dns")) {
                        String dnsServer = jsonResponse.getString("dns");
                        Log.i(getTag(), "DNS server: " + dnsServer);
                        return dnsServer;
                    } else {
                        Log.e(getTag(), "Error: DNS not found in the response.");
                        throw PVNClientException.fromMessage("Error: DNS not found in the response.");
                    }
                } else {
                    Log.e(getTag(), "Error response: " + response);
                }
            }

        } catch (JSONException | IOException e) {
            Log.e(getTag(), "Get DNS failed", e);
            throw PVNClientException.fromException(e);
        }
        return null;
    }

    public void startWebSocket(String host, int port, WebSocketListener webSocketListener) {
        if (!isValid(token)) {
            token = getAuthToken(host, port);
        }

        Request request = new Request.Builder()
                .url(String.format(Locale.getDefault(), WEBSOCKET_URL, host, port))
                .addHeader("Authorization", "Bearer " + token)
                //todo: ClientIP? Зачем?
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
        if (webSocket != null) {
            ByteString payload = ByteString.copyFrom(buffer, length);

            Protocol.IPPacket packet = Protocol.IPPacket.newBuilder()
                    .setPayload(payload)
                    .build();
            Protocol.Message msg = Protocol.Message.newBuilder()
                    .setProtocolVersion(1)
                    .setMsgType(Protocol.MessageType.MSG_IP_PACKET)
                    .setPacket(packet)
                    .build();

            webSocket.send(okio.ByteString.of(msg.toByteArray()));
        }
    }
}
