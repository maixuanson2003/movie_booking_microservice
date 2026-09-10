package com.example.email_service.service;

import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;

import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.example.email_service.exception.ResourceNotFoundException;
import com.example.email_service.model.EmailTemplate;
import com.example.email_service.repository.EmailTemplateRepository;
import com.example.email_service.sharedLogic.dto.SendEmailRequest;

@Service
public class EmailService extends BaseService<EmailTemplate, Long> {
    private final EmailTemplateRepository repository;
    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(EmailTemplateRepository emailRepository, JavaMailSender mailSender,
            @Value("${app.mail.from}") String from) {
        super(emailRepository);
        this.repository = emailRepository;
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendEmail(SendEmailRequest request) {
        String to = request.getTo();
        String subject = request.getSubject();
        String type = request.getType();

        EmailTemplate template = repository.findByTypeAndStatus(type, "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Active email template not found for type: " + type));
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setValidateAddresses(true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            String content = template.getBody();
            for (Entry<String, Object> entry : request.getVariables().entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                Assert.notNull(entry.getValue(), "Template variable must not be null");
                content = content.replace(placeholder,
                        org.springframework.web.util.HtmlUtils.htmlEscape(entry.getValue().toString()));
            }
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new MailPreparationException("Could not prepare email", ex);
        }

    }

}
