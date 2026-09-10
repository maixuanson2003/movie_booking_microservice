package com.example.email_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.email_service.model.EmailTemplate;
import com.example.email_service.sharedLogic.dto.EmailTemplateDTO;

@Component
public class EmailTemplateMapper extends BaseMapper<EmailTemplate, EmailTemplateDTO> {
    @Override
    public EmailTemplate toEntity(EmailTemplateDTO dto) {
        if (dto == null) return null;
        EmailTemplate entity = new EmailTemplate();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setSubject(dto.getSubject());
        entity.setBody(dto.getBody());
        entity.setType(dto.getType());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }

    @Override
    public EmailTemplateDTO toDto(EmailTemplate entity) {
        if (entity == null) return null;
        EmailTemplateDTO dto = new EmailTemplateDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSubject(entity.getSubject());
        dto.setBody(entity.getBody());
        dto.setType(entity.getType());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
