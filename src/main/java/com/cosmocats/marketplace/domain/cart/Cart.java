package com.cosmocats.marketplace.domain.cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Cart(UUID id, UUID customerId, List<CartItem> items) {

    public Cart {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        items = List.copyOf(items);
    }

    public BigDecimal totalPrice() {
        return items.stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
