package com.cosmocats.marketplace.domain.product;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Product offered on the marketplace.
 * The category is referenced by id because Category is a separate aggregate.
 * The id is null until the product is stored.
 */
public record Product(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        UUID categoryId
) {

    public Product {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(categoryId, "categoryId must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("price must be greater than 0");
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("stockQuantity must not be negative");
        }
    }

    public Product withId(UUID newId) {
        return new Product(newId, name, description, price, stockQuantity, categoryId);
    }
}
