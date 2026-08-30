package com.tns.mes.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class ExternalApiConfig {
    @Bean
    public RestTemplate externalRestTemplate(RestTemplateBuilder builder,
                                              @Value("${mes.integration.http.connect-timeout-ms:5000}") long connectTimeout,
                                              @Value("${mes.integration.http.read-timeout-ms:30000}") long readTimeout) {
        return builder.setConnectTimeout(Duration.ofMillis(connectTimeout)).setReadTimeout(Duration.ofMillis(readTimeout))
                .additionalMessageConverters(new org.springframework.http.converter.StringHttpMessageConverter(StandardCharsets.UTF_8))
                .build();
    }

    @Bean
    public ExternalApiClient externalApiClient(RestTemplate externalRestTemplate) { return new ExternalApiClient(externalRestTemplate); }
}
