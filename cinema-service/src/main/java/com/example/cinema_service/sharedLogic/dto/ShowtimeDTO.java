package com.example.cinema_service.sharedLogic.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ShowtimeDTO {
    private Long id;
    private Long movieId;
    private Long roomId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal price;
    private String status;
    private LocalDateTime createdAt;
}

