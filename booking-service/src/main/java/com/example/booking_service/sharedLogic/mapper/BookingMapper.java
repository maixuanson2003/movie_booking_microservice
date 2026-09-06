package com.example.booking_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.booking_service.model.Booking;
import com.example.booking_service.sharedLogic.dto.BookingDTO;

@Component
public class BookingMapper extends BaseMapper<Booking, BookingDTO> {
    @Override
    public Booking toEntity(BookingDTO dto) {
        if (dto == null) return null;
        Booking entity = new Booking();
        entity.setId(dto.getId());
        entity.setUserId(dto.getUserId());
        entity.setBookingCode(dto.getBookingCode());
        entity.setTotalAmount(dto.getTotalAmount());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setExpiresAt(dto.getExpiresAt());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }

    @Override
    public BookingDTO toDto(Booking entity) {
        if (entity == null) return null;
        BookingDTO dto = new BookingDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setBookingCode(entity.getBookingCode());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setStatus(entity.getStatus());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}

