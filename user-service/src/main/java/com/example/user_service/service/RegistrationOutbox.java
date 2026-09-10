package com.example.user_service.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import jakarta.persistence.EntityManager;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.example.user_service.model.User;

@Service
public class RegistrationOutbox {
    private final EntityManager entityManager;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RegistrationOutbox(EntityManager entityManager, KafkaTemplate<String, Object> kafkaTemplate) {
        this.entityManager = entityManager;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(User user) {
        entityManager.createNativeQuery("""
                INSERT INTO user_registration_outbox (event_id, user_id, username, email)
                VALUES (:eventId, :userId, :username, :email)
                """)
                .setParameter("eventId", UUID.randomUUID().toString())
                .setParameter("userId", user.getId())
                .setParameter("username", user.getUsername())
                .setParameter("email", user.getEmail())
                .executeUpdate();
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    @Transactional(rollbackFor = Exception.class)
    public void publishPending() throws Exception {
        // One row per transaction; concurrent instances skip rows already locked.
        var rows = entityManager.createNativeQuery("""
                SELECT event_id, user_id, username, email FROM user_registration_outbox
                ORDER BY created_at, event_id LIMIT 1 FOR UPDATE SKIP LOCKED
                """).getResultList();
        if (rows.isEmpty()) return;
        Object[] row = (Object[]) rows.get(0);
        String eventId = (String) row[0];
        try {
            kafkaTemplate.send("user-registered", row[1].toString(), Map.of(
                    "eventId", eventId, "id", row[1], "username", row[2], "email", row[3]))
                    .get(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        }
        entityManager.createNativeQuery("DELETE FROM user_registration_outbox WHERE event_id = :eventId")
                .setParameter("eventId", eventId).executeUpdate();
    }
}
