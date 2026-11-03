package com.cosmocats.marketplace.web.order.dto;

import com.cosmocats.marketplace.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponseDto(
        UUID cartId,
        UUID customerId,
        OrderStatus status,
        Instant createdAt,
        List<Item> items,
        BigDecimal totalPrice
) {

    public record Item(UUID productId, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }
}
