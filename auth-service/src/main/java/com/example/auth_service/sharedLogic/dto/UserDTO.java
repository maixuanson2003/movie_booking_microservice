package com.example.auth_service.sharedLogic.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String role;
    private String phone;
    private String avatar;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
