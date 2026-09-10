package com.example.email_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.email_service.exception.ResourceNotFoundException;
import com.example.email_service.model.EmailTemplate;
import com.example.email_service.repository.EmailTemplateRepository;

@Service
public class EmailTemplateService extends BaseService<EmailTemplate, Long> {

    private final EmailTemplateRepository emailTemplateRepository;

    public EmailTemplateService(EmailTemplateRepository emailTemplateRepository) {
        super(emailTemplateRepository);
        this.emailTemplateRepository = emailTemplateRepository;
    }

    @Transactional(readOnly = true)
    public EmailTemplate findByType(String type) {
        return emailTemplateRepository.findByType(type)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found with type: " + type));
    }
}
