package com.example.email_service.sharedLogic.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {
    private String to;
    private String subject;
    private String type;
    private Map<String, Object> variables = Map.of();
}
