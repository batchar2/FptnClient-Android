package com.filantrop.pvnclient.services.exception;

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

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
