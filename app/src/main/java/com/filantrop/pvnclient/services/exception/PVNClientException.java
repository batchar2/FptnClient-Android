package com.filantrop.pvnclient.services.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PVNClientException extends RuntimeException {
    private Exception exception;
    private String errorMessage;

    private PVNClientException(Exception exception, String errorMessage) {
        this.exception = exception;
        this.errorMessage = errorMessage;
    }

    public static PVNClientException fromException(Exception e) {
        return new PVNClientException(e, e.getMessage());
    }

    public static PVNClientException fromMessage(String message) {
        return new PVNClientException(null, message);
    }

}
