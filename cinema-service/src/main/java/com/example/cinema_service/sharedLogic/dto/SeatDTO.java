package com.example.cinema_service.sharedLogic.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class SeatDTO {
    private Long id;
    private Long roomId;
    private String seatNumber;
    private String rowName;
    private String seatType;
    private String status;
}

