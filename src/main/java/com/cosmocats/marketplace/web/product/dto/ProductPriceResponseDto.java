package com.cosmocats.marketplace.web.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductPriceResponseDto(
        UUID productId,
        BigDecimal basePrice,
        String baseCurrency,
        BigDecimal price,
        String currency,
        BigDecimal exchangeRate
) {
}
