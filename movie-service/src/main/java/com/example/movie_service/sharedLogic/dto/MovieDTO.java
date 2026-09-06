package com.example.movie_service.sharedLogic.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class MovieDTO {
    private Long id;
    private String title;
    private String description;
    private Integer duration;
    private LocalDate releaseDate;
    private String director;
    private String ageRating;
    private String posterUrl;
    private String trailerUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

