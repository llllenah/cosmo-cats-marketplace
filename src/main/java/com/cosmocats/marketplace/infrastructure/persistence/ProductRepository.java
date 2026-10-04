package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.application.product.PageCursor;
import com.cosmocats.marketplace.domain.product.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    Optional<Product> findByNameIgnoreCase(String name);

    /**
     * Returns up to {@code limit} products sorted by name and id, starting right after the cursor.
     * A null cursor means the first page.
     */
    List<Product> findPageAfter(PageCursor cursor, int limit);

    long count();

    boolean existsById(UUID id);

    void deleteById(UUID id);
}
