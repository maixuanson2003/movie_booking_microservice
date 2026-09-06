package com.example.booking_service.exception;

import org.springframework.http.HttpStatus;

public class UpstreamServiceException extends BusinessException {
    public UpstreamServiceException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}
