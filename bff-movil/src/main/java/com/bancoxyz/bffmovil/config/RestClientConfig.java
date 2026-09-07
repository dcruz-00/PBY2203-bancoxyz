package com.bancoxyz.bffmovil.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Bean
    public RestClient coreApiClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8080")
                .defaultHeader("X-Internal-Key", internalApiKey)
                .build();
    }
}