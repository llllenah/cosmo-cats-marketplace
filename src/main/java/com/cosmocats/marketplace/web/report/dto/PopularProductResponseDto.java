package com.cosmocats.marketplace.web.report.dto;

import java.util.UUID;

public record PopularProductResponseDto(UUID productId, String productName, long totalQuantity, long orderCount) {
}
