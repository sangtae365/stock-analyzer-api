package com.stockanalyzer.exception;

public class KisApiException extends RuntimeException {

    private final int statusCode;

    public KisApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public KisApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
