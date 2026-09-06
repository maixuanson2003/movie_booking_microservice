package com.example.cinema_service.sharedLogic.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class RoomDTO {
    private Long id;
    private Long cinemaId;
    private String name;
    private Integer capacity;
    private String roomType;
    private String status;
}

