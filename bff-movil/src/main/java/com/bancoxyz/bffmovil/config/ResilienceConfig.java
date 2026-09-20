package com.bancoxyz.bffmovil.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    public static final String CORE_API_CB = "core-api";

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> coreApiCircuitBreakerCustomizer() {
        return factory -> factory.configure(builder -> builder
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(5)
                        .minimumNumberOfCalls(3)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(15))
                        .permittedNumberOfCallsInHalfOpenState(2)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .build()),
                CORE_API_CB);
    }
}