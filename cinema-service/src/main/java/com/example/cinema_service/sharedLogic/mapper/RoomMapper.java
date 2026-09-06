package com.example.cinema_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.cinema_service.model.Room;
import com.example.cinema_service.sharedLogic.dto.RoomDTO;

@Component
public class RoomMapper extends BaseMapper<Room, RoomDTO> {
    @Override
    public Room toEntity(RoomDTO dto) {
        if (dto == null) return null;
        Room entity = new Room();
        entity.setId(dto.getId());
        entity.setCinemaId(dto.getCinemaId());
        entity.setName(dto.getName());
        entity.setCapacity(dto.getCapacity());
        entity.setRoomType(dto.getRoomType());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        return entity;
    }

    @Override
    public RoomDTO toDto(Room entity) {
        if (entity == null) return null;
        RoomDTO dto = new RoomDTO();
        dto.setId(entity.getId());
        dto.setCinemaId(entity.getCinemaId());
        dto.setName(entity.getName());
        dto.setCapacity(entity.getCapacity());
        dto.setRoomType(entity.getRoomType());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}

