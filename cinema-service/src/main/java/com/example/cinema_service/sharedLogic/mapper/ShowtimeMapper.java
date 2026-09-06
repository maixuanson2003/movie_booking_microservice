package com.example.cinema_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.cinema_service.model.Showtime;
import com.example.cinema_service.sharedLogic.dto.ShowtimeDTO;

@Component
public class ShowtimeMapper extends BaseMapper<Showtime, ShowtimeDTO> {
    @Override
    public Showtime toEntity(ShowtimeDTO dto) {
        if (dto == null) return null;
        Showtime entity = new Showtime();
        entity.setId(dto.getId());
        entity.setMovieId(dto.getMovieId());
        entity.setRoomId(dto.getRoomId());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setPrice(dto.getPrice());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setCreatedAt(dto.getCreatedAt());
        return entity;
    }

    @Override
    public ShowtimeDTO toDto(Showtime entity) {
        if (entity == null) return null;
        ShowtimeDTO dto = new ShowtimeDTO();
        dto.setId(entity.getId());
        dto.setMovieId(entity.getMovieId());
        dto.setRoomId(entity.getRoomId());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setPrice(entity.getPrice());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}

