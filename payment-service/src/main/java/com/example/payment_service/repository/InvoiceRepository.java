package com.example.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.payment_service.model.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}

