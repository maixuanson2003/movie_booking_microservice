package com.example.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.payment_service.model.PaymentMethod;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
}

