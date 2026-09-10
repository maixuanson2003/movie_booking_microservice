package com.example.email_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaProducerConfig {
    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties properties) {
        // Deserialization failures carry raw bytes; preserve them when publishing to DLT.
        return new DefaultKafkaProducerFactory<>(properties.buildProducerProperties(),
                new org.apache.kafka.common.serialization.StringSerializer(),
                new org.springframework.kafka.support.serializer.DelegatingByTypeSerializer(java.util.Map.of(
                        byte[].class, new org.apache.kafka.common.serialization.ByteArraySerializer(),
                        Object.class, new org.springframework.kafka.support.serializer.JacksonJsonSerializer<>()), true));
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
