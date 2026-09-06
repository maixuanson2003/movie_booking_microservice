package com.example.cinema_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.cinema_service.model.Cinema;
import com.example.cinema_service.sharedLogic.dto.CinemaDTO;

@Component
public class CinemaMapper extends BaseMapper<Cinema, CinemaDTO> {
    @Override
    public Cinema toEntity(CinemaDTO dto) {
        if (dto == null) return null;
        Cinema entity = new Cinema();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setAddress(dto.getAddress());
        entity.setCity(dto.getCity());
        entity.setPhone(dto.getPhone());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setCreatedAt(dto.getCreatedAt());
        return entity;
    }

    @Override
    public CinemaDTO toDto(Cinema entity) {
        if (entity == null) return null;
        CinemaDTO dto = new CinemaDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setAddress(entity.getAddress());
        dto.setCity(entity.getCity());
        dto.setPhone(entity.getPhone());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}

