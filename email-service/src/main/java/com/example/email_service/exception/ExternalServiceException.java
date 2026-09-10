package com.example.email_service.exception;

public class ExternalServiceException extends RuntimeException {

    private final int status;
    private final Object data;

    public ExternalServiceException(
            int status,
            String message,
            Object data) {

        super(message);
        this.status = status;
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public Object getData() {
        return data;
    }

}
