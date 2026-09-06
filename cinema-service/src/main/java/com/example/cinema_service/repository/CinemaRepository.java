package com.example.cinema_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.cinema_service.model.Cinema;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {
}

