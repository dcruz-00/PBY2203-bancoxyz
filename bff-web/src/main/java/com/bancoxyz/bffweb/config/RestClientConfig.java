package com.bancoxyz.bffweb.config;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UncheckedIOException;

@Configuration
public class RestClientConfig {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Value("${server.ssl.key-store}")
    private Resource keyStore;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Bean
    public RestClient coreApiClient(CircuitBreakerFactory<?, ?> circuitBreakerFactory) throws Exception {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(keyStore.getURL(), keyStorePassword.toCharArray())
                .build();

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(socketFactory)
                                .build()
                )
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        CircuitBreaker circuitBreaker = circuitBreakerFactory.create(ResilienceConfig.CORE_API_CB);

        return RestClient.builder()
                .baseUrl("https://localhost:8080")
                .defaultHeader("X-Internal-Key", internalApiKey)
                .requestFactory(requestFactory)
                .requestInterceptor(circuitBreakerInterceptor(circuitBreaker))
                .build();
    }

    private ClientHttpRequestInterceptor circuitBreakerInterceptor(CircuitBreaker circuitBreaker) {
        return (request, body, execution) -> circuitBreaker.run(
                () -> {
                    try {
                        return execution.execute(request, body);
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                },
                throwable -> {
                    if (throwable instanceof CallNotPermittedException) {
                        throw new ResourceAccessException(
                                "circuito abierto, llamada rechazada sin contactar al servicio");
                    }
                    Throwable causa = (throwable instanceof UncheckedIOException && throwable.getCause() != null)
                            ? throwable.getCause()
                            : throwable;
                    throw new ResourceAccessException(
                            "sin respuesta (" + causa.getClass().getSimpleName() + ")");
                });
    }
}