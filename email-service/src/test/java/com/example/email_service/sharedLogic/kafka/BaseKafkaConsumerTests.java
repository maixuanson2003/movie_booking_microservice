package com.example.email_service.sharedLogic.kafka;

import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.example.email_service.service.EmailService;
import com.example.email_service.sharedLogic.dto.SendEmailRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseKafkaConsumerTests {
    private final EmailService service = mock(EmailService.class);
    private final BaseKafkaConsumer consumer = new BaseKafkaConsumer(new KafkaHandlerPool(service));

    @Test
    void routesRegistrationPayloadToEmail() {
        consumer.listenUserRegistered(new ConsumerRecord<>("user-registered", 0, 0, "42",
                Map.of("username", "An", "email", "an@example.com")));
        var request = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(service).sendEmail(request.capture());
        assertEquals("an@example.com", request.getValue().getTo());
        assertEquals("WELCOME_EMAIL", request.getValue().getType());
        assertEquals("An", request.getValue().getVariables().get("username"));
    }

    @Test
    void rejectsMalformedPayloadBeforeSending() {
        assertThrows(IllegalArgumentException.class, () -> consumer.listenUserRegistered(
                new ConsumerRecord<>("user-registered", 0, 0, null, Map.of("username", "An"))));
        verifyNoInteractions(service);
    }

    @Test
    void propagatesFailureToKafkaErrorHandler() {
        var failure = new org.springframework.mail.MailSendException("SMTP down");
        doThrow(failure).when(service).sendEmail(any());
        assertSame(failure, assertThrows(org.springframework.mail.MailSendException.class,
                () -> consumer.listenUserRegistered(new ConsumerRecord<>("user-registered", 0, 0, "42",
                        Map.of("username", "An", "email", "an@example.com")))));
    }
}
