package com.example.booking_service.sharedLogic.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class ApiResponse {
    private String message;
    private Object data;
    private boolean success;

    public ApiResponse(String message, Object data, boolean success) {
        this.message = message;
        this.data = data;
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public boolean isSuccess() {
        return success;
    }

}
