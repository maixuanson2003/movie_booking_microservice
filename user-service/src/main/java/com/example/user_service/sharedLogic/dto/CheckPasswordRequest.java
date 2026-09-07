package com.example.user_service.sharedLogic.dto;

/** Credentials are accepted in the request body, never in the URL. */
public record CheckPasswordRequest(String username, String password) {
    @Override
    public String toString() {
        return "CheckPasswordRequest[credentials=REDACTED]";
    }
}
