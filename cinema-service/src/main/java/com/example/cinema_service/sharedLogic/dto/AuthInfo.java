package com.example.cinema_service.sharedLogic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthInfo {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
}

