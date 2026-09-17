package com.alvaro.eventbridge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Configuration
public class AwsConfig {

    @Bean
    public EventBridgeClient eventBridgeClient(@Value("${aws.region:us-east-1}") String region) {
        return EventBridgeClient.builder()
                .region(Region.of(region))
                .build();
    }
}
