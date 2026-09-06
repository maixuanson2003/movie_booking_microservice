package com.example.cinema_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.cinema_service.model.Showtime;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
}

