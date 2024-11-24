package com.filantrop.pvnclient.exception;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PVNClientException extends RuntimeException {
    private final Exception exception;
    private final String errorMessage;

    public static PVNClientException fromException(Exception e) {
        return PVNClientException.builder().errorMessage(e.getMessage()).exception(e).build();
    }

    public static PVNClientException fromMessage(String message) {
        return PVNClientException.builder().errorMessage(message).exception(null).build();
    }
}
