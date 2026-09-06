package com.example.booking_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.booking_service.model.Combo;

public interface ComboRepository extends JpaRepository<Combo, Long> {
}

