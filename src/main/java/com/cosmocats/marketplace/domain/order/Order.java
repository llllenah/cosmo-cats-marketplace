package com.cosmocats.marketplace.domain.order;

import com.cosmocats.marketplace.domain.cart.Cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Order(
        UUID id,
        UUID customerId,
        List<OrderItem> items,
        OrderStatus status,
        Instant createdAt
) {

    public Order {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        items = List.copyOf(items);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
    }

    public static Order placeFrom(Cart cart, Instant createdAt) {
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("cannot place an order from an empty cart");
        }
        List<OrderItem> orderItems = cart.items().stream()
                .map(item -> new OrderItem(item.productId(), item.quantity(), item.unitPrice()))
                .toList();
        return new Order(UUID.randomUUID(), cart.customerId(), orderItems, OrderStatus.CREATED, createdAt);
    }

    public BigDecimal totalPrice() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
