package com.example.email_service.service;

import java.util.Optional;
import java.util.Map;
import com.example.email_service.sharedLogic.dto.SendEmailRequest;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import com.example.email_service.exception.ResourceNotFoundException;
import com.example.email_service.model.EmailTemplate;
import com.example.email_service.repository.EmailTemplateRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTests {
    private final EmailTemplateRepository repository = mock(EmailTemplateRepository.class);
    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final EmailService service = new EmailService(repository, sender, "sender@example.com");

    private MimeMessage prepareTemplate() {
        EmailTemplate template = new EmailTemplate();
        template.setBody("<p>Đặt vé thành công</p>");
        when(repository.findByTypeAndStatus("BOOKING_CONFIRMATION", "ACTIVE"))
                .thenReturn(Optional.of(template));
        MimeMessage message = new MimeMessage((Session) null);
        when(sender.createMimeMessage()).thenReturn(message);
        return message;
    }

    @Test
    void sendsHtmlWithUtf8AndConfiguredSender() throws Exception {
        MimeMessage message = prepareTemplate();
        service.sendEmail(new SendEmailRequest("customer@example.com", "Xác nhận đặt vé", "BOOKING_CONFIRMATION", Map.of()));
        message.saveChanges();
        assertEquals("sender@example.com", message.getFrom()[0].toString());
        assertEquals("customer@example.com", message.getAllRecipients()[0].toString());
        assertEquals("Xác nhận đặt vé", message.getSubject());
        assertEquals("<p>Đặt vé thành công</p>", message.getContent());
        assertTrue(message.isMimeType("text/html"));
        assertTrue(message.getContentType().toUpperCase().contains("UTF-8"));
        verify(sender).send(message);
    }

    @Test
    void rendersVariablesWithoutReplacingSubjectInBody() throws Exception {
        MimeMessage message = prepareTemplate();
        EmailTemplate template = new EmailTemplate();
        template.setBody("<p>Subject {{username}}</p>");
        when(repository.findByTypeAndStatus("BOOKING_CONFIRMATION", "ACTIVE"))
                .thenReturn(Optional.of(template));
        service.sendEmail(new SendEmailRequest("customer@example.com", "Subject", "BOOKING_CONFIRMATION",
                Map.of("username", "<An>")));
        assertEquals("<p>Subject &lt;An&gt;</p>", message.getContent());
    }

    @Test
    void missingOrInactiveTemplateDoesNotSend() {
        when(repository.findByTypeAndStatus("MISSING", "ACTIVE")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.sendEmail(new SendEmailRequest("customer@example.com", "Subject", "MISSING", Map.of())));
        verifyNoInteractions(sender);
    }

    @Test
    void propagatesSmtpFailureToCaller() {
        MimeMessage message = prepareTemplate();
        MailSendException failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(sender).send(message);
        assertSame(failure, assertThrows(MailSendException.class,
                () -> service.sendEmail(new SendEmailRequest("customer@example.com", "Subject", "BOOKING_CONFIRMATION", Map.of()))));
    }
}
