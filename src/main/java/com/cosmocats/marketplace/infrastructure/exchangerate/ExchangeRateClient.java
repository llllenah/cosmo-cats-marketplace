package com.cosmocats.marketplace.infrastructure.exchangerate;

import java.math.BigDecimal;

public interface ExchangeRateClient {

    /**
     * Returns how many units of {@code targetCurrency} one unit of {@code baseCurrency} costs.
     *
     * @throws com.cosmocats.marketplace.application.exception.UnsupportedCurrencyException
     *         if the service does not know the currency
     * @throws com.cosmocats.marketplace.application.exception.ExchangeRateUnavailableException
     *         if the service fails, times out or returns an invalid rate
     */
    BigDecimal getRate(String baseCurrency, String targetCurrency);
}
