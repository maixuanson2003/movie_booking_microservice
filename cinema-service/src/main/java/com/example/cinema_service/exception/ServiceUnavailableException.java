package com.example.cinema_service.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
