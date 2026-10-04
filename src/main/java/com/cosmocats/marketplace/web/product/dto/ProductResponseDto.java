package com.cosmocats.marketplace.web.product.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponseDto(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        UUID categoryId
) {
}
