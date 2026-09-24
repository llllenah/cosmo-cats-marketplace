package com.cosmocats.marketplace.domain.cart;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record CartItem(UUID productId, int quantity, BigDecimal unitPrice) {

    public CartItem {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than 0");
        }
        if (unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("unitPrice must be greater than 0");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
