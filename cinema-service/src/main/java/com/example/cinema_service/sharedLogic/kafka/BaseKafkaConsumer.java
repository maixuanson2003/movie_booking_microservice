package com.example.cinema_service.sharedLogic.kafka;

import java.util.Map;
import java.util.function.Consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class BaseKafkaConsumer {

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
}
