package com.example.cinema_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.cinema_service.model.Seat;
import com.example.cinema_service.sharedLogic.dto.SeatDTO;

@Component
public class SeatMapper extends BaseMapper<Seat, SeatDTO> {
    @Override
    public Seat toEntity(SeatDTO dto) {
        if (dto == null) return null;
        Seat entity = new Seat();
        entity.setId(dto.getId());
        entity.setRoomId(dto.getRoomId());
        entity.setSeatNumber(dto.getSeatNumber());
        entity.setRowName(dto.getRowName());
        entity.setSeatType(dto.getSeatType());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        return entity;
    }

    @Override
    public SeatDTO toDto(Seat entity) {
        if (entity == null) return null;
        SeatDTO dto = new SeatDTO();
        dto.setId(entity.getId());
        dto.setRoomId(entity.getRoomId());
        dto.setSeatNumber(entity.getSeatNumber());
        dto.setRowName(entity.getRowName());
        dto.setSeatType(entity.getSeatType());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}

