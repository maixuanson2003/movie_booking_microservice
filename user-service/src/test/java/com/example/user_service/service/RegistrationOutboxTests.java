package com.example.user_service.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationOutboxTests {
    private final EntityManager em = mock(EntityManager.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final RegistrationOutbox outbox = new RegistrationOutbox(em, kafka);

    private void pendingEvent() {
        Query select = mock(Query.class);
        when(em.createNativeQuery(startsWith("SELECT"))).thenReturn(select);
        when(select.getResultList()).thenReturn(List.<Object[]>of(new Object[]{"event-1", 42L, "An", "an@example.com"}));
    }

    @Test
    void deletesOnlyAfterBrokerAcknowledges() throws Exception {
        pendingEvent();
        when(kafka.send(eq("user-registered"), eq("42"), any())).thenReturn(CompletableFuture.completedFuture(null));
        Query delete = mock(Query.class);
        when(em.createNativeQuery(startsWith("DELETE"))).thenReturn(delete);
        when(delete.setParameter("eventId", "event-1")).thenReturn(delete);
        outbox.publishPending();
        var order = inOrder(kafka, delete);
        order.verify(kafka).send(eq("user-registered"), eq("42"), any());
        order.verify(delete).setParameter("eventId", "event-1");
        order.verify(delete).executeUpdate();
    }

    @Test
    void keepsPendingEventWhenBrokerFails() {
        pendingEvent();
        when(kafka.send(eq("user-registered"), eq("42"), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Broker down")));
        assertThrows(java.util.concurrent.ExecutionException.class, outbox::publishPending);
        verify(em, never()).createNativeQuery(startsWith("DELETE"));
    }
}
