package com.example.auth_service.sharedLogic.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResult(UserDTO userDto, @JsonProperty("isLogin") boolean isLogin) {
}
