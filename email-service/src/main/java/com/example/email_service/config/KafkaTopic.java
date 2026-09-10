package com.example.email_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopic {
    @Bean
    public NewTopic userRegisteredTopic() {
        return TopicBuilder
                .name("user-registered")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
