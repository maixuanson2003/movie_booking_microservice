package com.example.booking_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.booking_service.model.BookingItem;

public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {
}

