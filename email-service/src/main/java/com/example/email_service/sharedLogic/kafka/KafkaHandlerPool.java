package com.example.email_service.sharedLogic.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

import com.example.email_service.service.EmailService;
import com.example.email_service.sharedLogic.dto.SendEmailRequest;


@Component
public class KafkaHandlerPool {

    private final Map<String, Consumer<SendEmailRequest>> handlers = new HashMap<>();

    public KafkaHandlerPool(EmailService emailService) {

        handlers.put(
                "user-registered",
                emailService::sendEmail);
    }

    public void execute(
            String eventType,
            SendEmailRequest data) {

        Consumer<SendEmailRequest> handler = handlers.get(eventType);

        if (handler == null) {
            throw new IllegalArgumentException(
                    "Unknown event type: " + eventType);
        }

        handler.accept(data);
    }
}
