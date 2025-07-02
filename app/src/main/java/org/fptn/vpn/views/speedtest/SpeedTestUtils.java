package org.fptn.vpn.views.speedtest;

import android.util.Log;

import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SpeedTestUtils {
    private static final String TAG = SpeedTestUtils.class.getName();
    private static final long SEARCH_BEST_SERVER_MAX_TIMEOUT = 6L;

    public static FptnServerDto findFastestServer(List<FptnServerDto> fptnServerDtoList, String sniHostName) throws PVNClientException {
        Log.d(TAG, "SpeedTestUtils.findFastestServer() start: " + Instant.now() + ", Thread.Id: " + Thread.currentThread().getId());
        if (fptnServerDtoList != null && !fptnServerDtoList.isEmpty()) {
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<NativeSpeedTestTask> nativeSpeedTestTaskList = fptnServerDtoList.stream()
                    .map(fptnServerDto -> new NativeSpeedTestTask(fptnServerDto, sniHostName))
                    .collect(Collectors.toList());
            try {
                List<Future<NativeSpeedTestResult>> futures = executor.invokeAll(nativeSpeedTestTaskList, SEARCH_BEST_SERVER_MAX_TIMEOUT, TimeUnit.SECONDS);
                List<NativeSpeedTestResult> nativeSpeedTestResults = futures.stream().filter(Future::isDone)
                        .map(nativeSpeedTestResultFuture -> {
                            try {
                                return nativeSpeedTestResultFuture.get();
                            } catch (ExecutionException | CancellationException |
                                     InterruptedException e) {
                                Log.e(TAG, "nativeSpeedTestResultFuture exception: " + e.getMessage(), e);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                Optional<NativeSpeedTestResult> bestTimeOptional = nativeSpeedTestResults.stream()
                        .filter(NativeSpeedTestResult::isSuccess)
                        .min(Comparator.comparingLong(NativeSpeedTestResult::getDurationsMillis));
                if (bestTimeOptional.isPresent()) {
                    NativeSpeedTestResult nativeSpeedTestResult = bestTimeOptional.get();
                    if (nativeSpeedTestResult.isSuccess()) {
                        Log.d(TAG, "SpeedTestUtils.findFastestServer()  bestServer: " + nativeSpeedTestResult.getFptnServerDto().getServerInfo() +
                                " with response time: " + nativeSpeedTestResult.getDurationsMillis() + " ms");
                        Log.d(TAG, "SpeedTestUtils.findFastestServer() end: " + Instant.now());
                        return nativeSpeedTestResult.getFptnServerDto();
                    }
                } else {
                    throw new PVNClientException(ErrorCode.ALL_SERVERS_UNREACHABLE);
                }

            } catch (InterruptedException e) {
                throw new PVNClientException(ErrorCode.FIND_FASTEST_SERVER_TIMEOUT);
            }
        }
        throw new PVNClientException(ErrorCode.SERVER_LIST_NULL_OR_EMPTY);
    }
}
