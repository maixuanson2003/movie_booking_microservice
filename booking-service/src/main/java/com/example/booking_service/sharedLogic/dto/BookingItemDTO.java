package com.example.booking_service.sharedLogic.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class BookingItemDTO {
    private Long id;
    private Long bookingId;
    private Long showtimeId;
    private Long seatId;
    private Long comboId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

