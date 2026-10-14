package com.cosmocats.marketplace.infrastructure.exchangerate;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("application.exchange-rate-service")
public record ExchangeRateProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
