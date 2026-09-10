package com.example.email_service.sharedLogic.kafka;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import com.example.email_service.sharedLogic.dto.SendEmailRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class BaseKafkaConsumer {
    private final KafkaHandlerPool handlerPool;

    BaseKafkaConsumer(KafkaHandlerPool handlerPool) {
        this.handlerPool = handlerPool;
    }

    /**
     * Called by a business @KafkaListener. Failures propagate to the container;
     * returning normally lets the configured RECORD ack mode commit the offset.
     * A null payload represents a Kafka tombstone and is passed to the handler.
     */
    public void consume(ConsumerRecord<String, Map<String, Object>> record,
            Consumer<Map<String, Object>> handler) {
        Assert.notNull(record, "Record must not be null");
        Assert.notNull(handler, "Handler must not be null");
        handler.accept(record.value());
    }

    @KafkaListener(topics = "user-registered")
    public void listenUserRegistered(ConsumerRecord<String, Map<String, Object>> record) {

        String username = (String) record.value().get("username");
        String useremail = (String) record.value().get("email");

        this.handlerPool.execute("user-registered",
                new SendEmailRequest(useremail, "Welcome to Our Service", "WELCOME_EMAIL",
                        Map.of("username", username, "email", useremail)));
    }

}
