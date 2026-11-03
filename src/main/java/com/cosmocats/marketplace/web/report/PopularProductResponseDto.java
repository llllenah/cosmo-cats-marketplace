package com.cosmocats.marketplace.web.report;

import java.util.UUID;

public record PopularProductResponseDto(UUID productId, String productName, long totalQuantity, long orderCount) {
}
