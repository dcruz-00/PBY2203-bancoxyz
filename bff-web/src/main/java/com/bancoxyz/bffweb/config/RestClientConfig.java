package com.bancoxyz.bffweb.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;

@Configuration
public class RestClientConfig {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Value("${server.ssl.key-store}")
    private Resource keyStore;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Bean
    public RestClient coreApiClient() throws Exception {
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

        return RestClient.builder()
                .baseUrl("https://localhost:8080")
                .defaultHeader("X-Internal-Key", internalApiKey)
                .requestFactory(requestFactory)
                .build();
    }
}