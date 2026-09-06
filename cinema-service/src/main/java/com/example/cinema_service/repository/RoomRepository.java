package com.example.cinema_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.cinema_service.model.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
}

