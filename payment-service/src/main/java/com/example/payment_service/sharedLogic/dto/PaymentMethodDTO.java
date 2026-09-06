package com.example.payment_service.sharedLogic.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class PaymentMethodDTO {
    private Long id;
    private String name;
    private String displayName;
    private String providerCode;
    private String apiUrl;
    private String webhookUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

