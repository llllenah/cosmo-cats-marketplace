package com.cosmocats.marketplace.domain.product;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Product offered on the marketplace.
 * The category is referenced by id because Category is a separate aggregate.
 * Input rules are checked before construction (ProductMapper and ProductValidationService),
 * the constructor only guards against missing required values.
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
    }

    public Product withId(UUID newId) {
        return new Product(newId, name, description, price, stockQuantity, categoryId);
    }
}
