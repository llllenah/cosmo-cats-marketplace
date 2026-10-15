package com.cosmocats.marketplace.infrastructure.exchangerate;

import java.math.BigDecimal;

/**
 * Response body of the exchange rate service: GET /v1/rates/{base}/{target}.
 */
record ExchangeRateResponse(String base, String target, BigDecimal rate) {
}
