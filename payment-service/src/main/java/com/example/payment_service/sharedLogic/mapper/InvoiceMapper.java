package com.example.payment_service.sharedLogic.mapper;

import org.springframework.stereotype.Component;
import com.example.payment_service.model.Invoice;
import com.example.payment_service.sharedLogic.dto.InvoiceDTO;

@Component
public class InvoiceMapper extends BaseMapper<Invoice, InvoiceDTO> {
    @Override
    public Invoice toEntity(InvoiceDTO dto) {
        if (dto == null) return null;
        Invoice entity = new Invoice();
        entity.setId(dto.getId());
        entity.setUserId(dto.getUserId());
        entity.setBookingId(dto.getBookingId());
        entity.setInvoiceNumber(dto.getInvoiceNumber());
        entity.setPaymentMethodId(dto.getPaymentMethodId());
        entity.setSubtotal(dto.getSubtotal());
        if (dto.getDiscountAmount() != null) entity.setDiscountAmount(dto.getDiscountAmount());
        if (dto.getTaxAmount() != null) entity.setTaxAmount(dto.getTaxAmount());
        entity.setTotalAmount(dto.getTotalAmount());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        entity.setIssuedAt(dto.getIssuedAt());
        entity.setDueAt(dto.getDueAt());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }

    @Override
    public InvoiceDTO toDto(Invoice entity) {
        if (entity == null) return null;
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setBookingId(entity.getBookingId());
        dto.setInvoiceNumber(entity.getInvoiceNumber());
        dto.setPaymentMethodId(entity.getPaymentMethodId());
        dto.setSubtotal(entity.getSubtotal());
        dto.setDiscountAmount(entity.getDiscountAmount());
        dto.setTaxAmount(entity.getTaxAmount());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setStatus(entity.getStatus());
        dto.setIssuedAt(entity.getIssuedAt());
        dto.setDueAt(entity.getDueAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}

