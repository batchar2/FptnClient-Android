package org.fptn.vpn.views.speedtest;

import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class NativeSpeedTestResult {
    FptnServerDto fptnServerDto;
    long durationsMillis;
    PVNClientException exception;

    public boolean isSuccess(){
        return exception == null;
    }
}
