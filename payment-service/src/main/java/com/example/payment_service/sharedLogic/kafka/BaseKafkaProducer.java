package com.example.payment_service.sharedLogic.kafka;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class BaseKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public BaseKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object payload) {
        Assert.hasText(topic, "Topic must not be blank");
        return kafkaTemplate.send(topic, key, payload);
    }

    public CompletableFuture<SendResult<String, Object>> send(String topic, Object payload) {
        return send(topic, null, payload);
    }
}
