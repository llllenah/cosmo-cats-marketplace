package com.cosmocats.marketplace.infrastructure.exchangerate;

import com.cosmocats.marketplace.application.exception.ExchangeRateUnavailableException;
import com.cosmocats.marketplace.application.exception.UnsupportedCurrencyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestExchangeRateClient implements ExchangeRateClient {

    private final RestClient exchangeRateRestClient;

    @Override
    public BigDecimal getRate(String baseCurrency, String targetCurrency) {
        ExchangeRateResponse response = fetchRate(baseCurrency, targetCurrency);
        if (response == null || response.rate() == null || response.rate().signum() <= 0) {
            log.warn("Exchange rate service returned no valid rate for {}->{}: {}", baseCurrency, targetCurrency, response);
            throw new ExchangeRateUnavailableException("Exchange rate service returned an invalid rate", null);
        }
        return response.rate();
    }

    private ExchangeRateResponse fetchRate(String baseCurrency, String targetCurrency) {
        try {
            return exchangeRateRestClient.get()
                    .uri("/v1/rates/{base}/{target}", baseCurrency, targetCurrency)
                    .retrieve()
                    .body(ExchangeRateResponse.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new UnsupportedCurrencyException(targetCurrency);
        } catch (RestClientException ex) {
            // 5xx, other 4xx, timeouts, refused connections and unreadable bodies
            log.warn("Exchange rate service call {}->{} failed: {}", baseCurrency, targetCurrency, ex.getMessage());
            throw new ExchangeRateUnavailableException("Exchange rate service call failed", ex);
        }
    }
}
