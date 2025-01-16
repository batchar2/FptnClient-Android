package com.filantrop.pvnclient.views.speedtest;

import android.util.Log;

import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.utils.MySSLSocketFactory;
import com.filantrop.pvnclient.vpnclient.exception.PVNClientException;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class SpeedTestService {
    private static final String TAG = SpeedTestService.class.getName();
    private static final String GET_FILE_PATTERN = "https://%s:%d/api/v1/test/file.bin";
    private static final long TIMEOUT_MS = 5000L;
    private static final String[] CHROME_CIPHERS = new String[]{
            // ANDROID DOESNT SUPPORT ALL
            "ECDHE-RSA-AES128-GCM-SHA256",
            "ECDHE-ECDSA-AES256-GCM-SHA384",
            // "ECDHE-RSA-AES256-GCM-SHA384",
            // "ECDHE-ECDSA-CHACHA20-POLY1305",
            "ECDHE-RSA-CHACHA20-POLY1305"
            // "ECDHE-RSA-AES128-CBC-SHA",
            // "ECDHE-RSA-AES256-CBC-SHA",
            // "RSA-AES128-GCM-SHA256",
            // "RSA-AES256-GCM-SHA384",
            // "RSA-AES128-CBC-SHA",
            // "RSA-AES256-CBC-SHA",
            //"RSA-3DES-EDE-CBC-SHA"
    };

    private final OkHttpClient client;

    public SpeedTestService() throws PVNClientException {
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
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "SSLContext init failed", e);
            throw new PVNClientException(e.getMessage());
        }

        // Create an SSL socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = new MySSLSocketFactory(sslContext.getSocketFactory(), CHROME_CIPHERS);
        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier((hostname, session) -> true);

        // Set 5 second timeout
        builder.callTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        this.client = builder.build();
    }

    public FptnServerDto findFastestServer(List<FptnServerDto> fptnServerDtoList) throws InterruptedException, PVNClientException {
        Log.d(TAG, "findFastestServer start: " + Instant.now());
        if (fptnServerDtoList != null && !fptnServerDtoList.isEmpty()) {
            CountDownLatch countDownLatch = new CountDownLatch(fptnServerDtoList.size());
            List<SpeedTestCallback> speedTestCallbacks = fptnServerDtoList.stream().map(fptnServerDto -> runServerSpeedTest(fptnServerDto, countDownLatch)).collect(Collectors.toList());
            boolean awaited = countDownLatch.await(6000, TimeUnit.MILLISECONDS);
            if (awaited) {
                speedTestCallbacks.forEach(speedTestCallback ->
                        Log.d(TAG, "Server: " + speedTestCallback.getFptnServerDto() +
                                ", response time: " + speedTestCallback.getDurationsMillis() + " ms"));
                Optional<SpeedTestCallback> bestTimeOptional = speedTestCallbacks.stream().filter(SpeedTestCallback::isSuccess).min(Comparator.comparingLong(SpeedTestCallback::getDurationsMillis));
                if (bestTimeOptional.isPresent()) {
                    SpeedTestCallback bestTimeServerCallback = bestTimeOptional.get();
                    FptnServerDto bestServer = bestTimeServerCallback.getFptnServerDto();
                    Log.d(TAG, "Best server: " + bestServer.getServerInfo() +
                            " with response time: " + bestTimeServerCallback.getDurationsMillis() + " ms");
                    Log.d(TAG, "findFastestServer end: " + Instant.now());
                    return bestServer;
                } else {
                    throw new PVNClientException("all servers are unreachable!");
                }
            } else {
                throw new PVNClientException("findFastestServer timeout exception");
            }
        } else {
            throw new PVNClientException("fptnServerDtoList is null or empty");
        }
    }

    private SpeedTestCallback runServerSpeedTest(FptnServerDto fptnServerDto, CountDownLatch countDownLatch) {
        String url = String.format(Locale.getDefault(), GET_FILE_PATTERN, fptnServerDto.getHost(), fptnServerDto.getPort());
        Request request = new Request.Builder().url(url).get().build();
        SpeedTestCallback callback = new SpeedTestCallback(fptnServerDto, Instant.now(), countDownLatch);
        client.newCall(request).enqueue(callback);
        return callback;
    }
}
