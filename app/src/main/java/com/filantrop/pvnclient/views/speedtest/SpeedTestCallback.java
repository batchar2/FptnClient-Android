package com.filantrop.pvnclient.views.speedtest;

import androidx.annotation.NonNull;

import com.filantrop.pvnclient.database.model.FptnServerDto;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SpeedTestCallback implements Callback {
    private final FptnServerDto fptnServerDto;
    private final Instant startTime;
    private final CountDownLatch countDownLatch;

    private Instant endTime;
    private Exception exception;

    public SpeedTestCallback(FptnServerDto fptnServerDto, Instant startTime, CountDownLatch countDownLatch) {
        this.fptnServerDto = fptnServerDto;
        this.startTime = startTime;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onFailure(@NonNull Call call, @NonNull IOException e) {
        exception = e;
        countDownLatch.countDown();
    }

    @Override
    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
        endTime = Instant.now();
        countDownLatch.countDown();

        Optional.of(response).map(Response::body).ifPresent(ResponseBody::close);
    }

    public boolean isSuccess() {
        return exception == null && endTime != null;
    }

    public long getDurationsMillis() {
        if (isSuccess()) {
            return Duration.between(startTime, endTime).toMillis();
        } else {
            return Long.MAX_VALUE;
        }
    }

    public FptnServerDto getFptnServerDto() {
        return fptnServerDto;
    }

}
