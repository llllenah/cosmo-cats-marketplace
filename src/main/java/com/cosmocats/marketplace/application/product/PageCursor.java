package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.domain.product.Product;

import java.util.Objects;
import java.util.UUID;

/**
 * Position in the product list for keyset pagination: name and id of the last product on a page.
 * Products are sorted by the database by lower(name) and then by id (ProductRepository#findPageAfter).
 */
public record PageCursor(String name, UUID id) {

    public PageCursor {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(id, "id must not be null");
    }

    public static PageCursor after(Product product) {
        return new PageCursor(product.name(), product.id());
    }
}
