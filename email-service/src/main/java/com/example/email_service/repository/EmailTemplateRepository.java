package com.example.email_service.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.email_service.model.EmailTemplate;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    Optional<EmailTemplate> findByName(String name);

    Optional<EmailTemplate> findByType(String type);

    Optional<EmailTemplate> findByTypeAndStatus(String type, String status);
}
