package com.example.cinema_service.sharedLogic.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class CinemaDTO {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String phone;
    private String status;
    private LocalDateTime createdAt;
}

