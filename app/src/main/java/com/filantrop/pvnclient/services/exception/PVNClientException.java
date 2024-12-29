package com.filantrop.pvnclient.services.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
@Data
public class PVNClientException extends RuntimeException {

    private Exception exception;

    private PVNClientException(String errorMessage, Exception exception) {
        super(errorMessage);
        this.exception = exception;
    }

    public static PVNClientException fromException(Exception e) {
        return new PVNClientException(e.getMessage(), e);
    }

    public static PVNClientException fromMessage(String message) {
        return new PVNClientException(message, null);
    }

}
