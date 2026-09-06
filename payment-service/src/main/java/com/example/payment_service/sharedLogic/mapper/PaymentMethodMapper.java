package com.example.payment_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.payment_service.model.PaymentMethod;
import com.example.payment_service.sharedLogic.dto.PaymentMethodDTO;

@Component
public class PaymentMethodMapper extends BaseMapper<PaymentMethod, PaymentMethodDTO> {
    @Override
    public PaymentMethod toEntity(PaymentMethodDTO dto) {
        if (dto == null) return null;
        PaymentMethod entity = new PaymentMethod();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDisplayName(dto.getDisplayName());
        entity.setProviderCode(dto.getProviderCode());
        entity.setApiUrl(dto.getApiUrl());
        entity.setWebhookUrl(dto.getWebhookUrl());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }

    @Override
    public PaymentMethodDTO toDto(PaymentMethod entity) {
        if (entity == null) return null;
        PaymentMethodDTO dto = new PaymentMethodDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDisplayName(entity.getDisplayName());
        dto.setProviderCode(entity.getProviderCode());
        dto.setApiUrl(entity.getApiUrl());
        dto.setWebhookUrl(entity.getWebhookUrl());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}

