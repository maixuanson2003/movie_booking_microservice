package com.example.movie_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.movie_service.model.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}

