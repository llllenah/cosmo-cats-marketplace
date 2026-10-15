package com.cosmocats.marketplace.application.pricing;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Product price converted from the base currency into the requested one.
 */
public record ProductPrice(
        UUID productId,
        BigDecimal basePrice,
        String baseCurrency,
        BigDecimal price,
        String currency,
        BigDecimal exchangeRate
) {
}
