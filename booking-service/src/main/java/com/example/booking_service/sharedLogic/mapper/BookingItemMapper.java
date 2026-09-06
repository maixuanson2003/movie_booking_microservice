package com.example.booking_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.booking_service.model.BookingItem;
import com.example.booking_service.sharedLogic.dto.BookingItemDTO;

@Component
public class BookingItemMapper extends BaseMapper<BookingItem, BookingItemDTO> {
    @Override
    public BookingItem toEntity(BookingItemDTO dto) {
        if (dto == null) return null;
        BookingItem entity = new BookingItem();
        entity.setId(dto.getId());
        entity.setBookingId(dto.getBookingId());
        entity.setShowtimeId(dto.getShowtimeId());
        entity.setSeatId(dto.getSeatId());
        entity.setComboId(dto.getComboId());
        if (dto.getQuantity() != null) entity.setQuantity(dto.getQuantity());
        entity.setPrice(dto.getPrice());
        entity.setTotalPrice(dto.getTotalPrice());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }

    @Override
    public BookingItemDTO toDto(BookingItem entity) {
        if (entity == null) return null;
        BookingItemDTO dto = new BookingItemDTO();
        dto.setId(entity.getId());
        dto.setBookingId(entity.getBookingId());
        dto.setShowtimeId(entity.getShowtimeId());
        dto.setSeatId(entity.getSeatId());
        dto.setComboId(entity.getComboId());
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        dto.setTotalPrice(entity.getTotalPrice());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}

