package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.domain.product.Product;

import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

/**
 * Position in the product list for keyset pagination: name and id of the last product on a page.
 * Products are always sorted by name (case-insensitive) and then by id.
 */
public record PageCursor(String name, UUID id) {

    public static final Comparator<Product> PRODUCT_ORDER = Comparator
            .comparing(Product::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Product::id);

    public PageCursor {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(id, "id must not be null");
    }

    public static PageCursor after(Product product) {
        return new PageCursor(product.name(), product.id());
    }

    public boolean isBefore(Product product) {
        int byName = String.CASE_INSENSITIVE_ORDER.compare(name, product.name());
        return byName != 0 ? byName < 0 : id.compareTo(product.id()) < 0;
    }
}
