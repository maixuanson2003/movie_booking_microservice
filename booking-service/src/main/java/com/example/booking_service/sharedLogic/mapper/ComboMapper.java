package com.example.booking_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.booking_service.model.Combo;
import com.example.booking_service.sharedLogic.dto.ComboDTO;

@Component
public class ComboMapper extends BaseMapper<Combo, ComboDTO> {
    @Override
    public Combo toEntity(ComboDTO dto) {
        if (dto == null) return null;
        Combo entity = new Combo();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setPrice(dto.getPrice());
        entity.setImageUrl(dto.getImageUrl());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }

    @Override
    public ComboDTO toDto(Combo entity) {
        if (entity == null) return null;
        ComboDTO dto = new ComboDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setPrice(entity.getPrice());
        dto.setImageUrl(entity.getImageUrl());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}

