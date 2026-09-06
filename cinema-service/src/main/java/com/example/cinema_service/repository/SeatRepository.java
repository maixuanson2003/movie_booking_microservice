package com.example.cinema_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.cinema_service.model.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {
}

