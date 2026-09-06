package com.example.booking_service.exception;

import org.springframework.http.HttpStatus;

public class GatewayTimeoutException extends BusinessException {
    public GatewayTimeoutException(String message) {
        super(message, HttpStatus.GATEWAY_TIMEOUT);
    }
}
