package com.filantrop.pvnclient.utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DataRateCalculator {
    private final long intervalMillis;
    private long bytes;
    private long lastUpdateTime;
    private long rate;
    private final Lock lock = new ReentrantLock();

    public DataRateCalculator(long intervalMillis) {
        this.intervalMillis = intervalMillis;
        this.bytes = 0;
        this.lastUpdateTime = System.currentTimeMillis();
        this.rate = 0;
    }

    public void update(long len) {
        lock.lock();
        try {
            long now = System.currentTimeMillis();
            long elapsed = now - lastUpdateTime;

            bytes += len;
            if (elapsed >= intervalMillis) {
                rate = bytes / (elapsed / 1000);
                lastUpdateTime = now;
                bytes = 0;
            }
        } finally {
            lock.unlock();
        }
    }

    public long getRateForSecond() {
        lock.lock();
        try {
            if (intervalMillis > 0) {
                return rate / (1000 / intervalMillis);
            }
            return 0;
        } finally {
            lock.unlock();
        }
    }

    public String getFormatString() {
        double bitsPerSec = getRateForSecond() * 8.0;
        String speedStr;
        if (bitsPerSec >= 1e9) {
            speedStr = String.format("%.2f Gbps", bitsPerSec / 1e9);
        } else if (bitsPerSec >= 1e6) {
            speedStr = String.format("%.2f Mbps", bitsPerSec / 1e6);
        } else if (bitsPerSec >= 1e3) {
            speedStr = String.format("%.2f Kbps", bitsPerSec / 1e3);
        } else {
            speedStr = String.format("%.2f bps", bitsPerSec);
        }
        return speedStr;
//        if (speedStr.length() >= 20) {
//            return speedStr;
//        } else {
//            return String.format("%-25s", speedStr);
//        }
    }
}