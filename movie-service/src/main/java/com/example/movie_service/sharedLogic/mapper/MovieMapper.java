package com.example.movie_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.movie_service.model.Movie;
import com.example.movie_service.sharedLogic.dto.MovieDTO;

@Component
public class MovieMapper extends BaseMapper<Movie, MovieDTO> {
    @Override
    public Movie toEntity(MovieDTO dto) {
        if (dto == null) return null;
        Movie entity = new Movie();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setDuration(dto.getDuration());
        entity.setReleaseDate(dto.getReleaseDate());
        entity.setDirector(dto.getDirector());
        entity.setAgeRating(dto.getAgeRating());
        entity.setPosterUrl(dto.getPosterUrl());
        entity.setTrailerUrl(dto.getTrailerUrl());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }

    @Override
    public MovieDTO toDto(Movie entity) {
        if (entity == null) return null;
        MovieDTO dto = new MovieDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setDuration(entity.getDuration());
        dto.setReleaseDate(entity.getReleaseDate());
        dto.setDirector(entity.getDirector());
        dto.setAgeRating(entity.getAgeRating());
        dto.setPosterUrl(entity.getPosterUrl());
        dto.setTrailerUrl(entity.getTrailerUrl());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}

