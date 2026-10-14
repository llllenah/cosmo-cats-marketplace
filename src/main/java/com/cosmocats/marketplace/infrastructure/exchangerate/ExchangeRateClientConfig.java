package com.cosmocats.marketplace.infrastructure.exchangerate;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ExchangeRateProperties.class)
public class ExchangeRateClientConfig {

    /**
     * Timeouts are required: without them a hanging exchange rate service would block
     * request threads of the marketplace.
     */
    @Bean
    public RestClient exchangeRateRestClient(RestClient.Builder builder, ExchangeRateProperties properties) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.connectTimeout())
                .withReadTimeout(properties.readTimeout());
        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
